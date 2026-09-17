package com.moasseum.app.notification

import android.app.Notification
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.moasseum.app.FinanceApplication
import com.moasseum.app.data.AiClient
import com.moasseum.app.domain.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PaymentNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val aiClient by lazy { AiClient() }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "notification listener connected")
        val financeApplication = application as? FinanceApplication
        if (financeApplication != null) {
            serviceScope.launch { financeApplication.preferencesRepository.markNotificationServiceConnected() }
        }
        runCatching { getActiveNotifications().orEmpty().forEach(::onNotificationPosted) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val financeApplication = application as? FinanceApplication ?: return

        serviceScope.launch {
            runCatching {
                // Record that the listener really received an external notification even when
                // the text is not a transaction. This makes the diagnostic state useful.
                financeApplication.preferencesRepository.markNotificationSeen()
                val content = runCatching { extractContent(sbn.notification) }
                    .onFailure { error -> Log.w(TAG, "notification text unavailable: ${error::class.java.simpleName}") }
                    .getOrDefault(NotificationContent("", ""))
                if (content.title.isBlank() && content.body.isBlank()) return@runCatching

                val aiEnabled = financeApplication.preferencesRepository.aiNotificationClassificationEnabled.first()
                val localCandidate = PaymentNotificationParser.parse(
                    packageName = sbn.packageName,
                    title = content.title,
                    body = content.body,
                    postedAt = sbn.postTime,
                )
                val shouldAskAi = aiEnabled && PaymentNotificationParser.shouldInspectWithAi(content.title, content.body)
                val candidate = if (!shouldAskAi) {
                    localCandidate
                } else {
                    // Never make a slow/unavailable AI endpoint stop local detection.
                    val classification = withTimeoutOrNull(AI_CLASSIFICATION_TIMEOUT_MS) {
                        aiClient.classifyNotification(
                            content.title.ifBlank { "알림" },
                            content.body,
                        ).getOrNull()
                    }
                    when {
                        classification == null -> localCandidate
                        !classification.isFinancialTransaction -> {
                            // Keep a notification with an unmistakable approval/deposit signal;
                            // this avoids losing real bank alerts when the model is conservative.
                            localCandidate?.takeIf {
                                PaymentNotificationParser.hasStrongTransactionSignal(content.title, content.body)
                            }
                        }
                        else -> PaymentNotificationParser.parse(
                            packageName = sbn.packageName,
                            title = content.title,
                            body = content.body,
                            postedAt = sbn.postTime,
                            aiConfirmedType = classification.transactionType,
                        ) ?: localCandidate
                    }
                }
                val detectedCandidate = candidate ?: return@runCatching
                val id = financeApplication.financeRepository.saveNotificationCandidate(detectedCandidate) ?: return@runCatching
                financeApplication.preferencesRepository.markNotificationCandidateCreated()
                val detected = detectedCandidate.copy(id = id).toDomain()
                if (financeApplication.isActivityVisible) {
                    financeApplication.publishNotificationCandidate(detected)
                } else {
                    PaymentNotificationNotifier.showCandidate(this@PaymentNotificationListenerService, detected)
                }
            }.onFailure { error ->
                Log.w(TAG, "notification processing failed: ${error::class.java.simpleName}")
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "notification listener disconnected; requesting rebind")
        (application as? FinanceApplication)?.let { financeApplication ->
            serviceScope.launch { financeApplication.preferencesRepository.markNotificationServiceDisconnected() }
        }
        // Android may disconnect a listener temporarily during a system settings
        // change or a One UI process restart. requestRebind is the only listener
        // API allowed in this callback, so retry a few times while this instance
        // is still alive. The activity also performs the same bounded retry when
        // it resumes after the user returns from Settings.
        serviceScope.launch {
            repeat(REBIND_ATTEMPTS) { attempt ->
                runCatching {
                    NotificationListenerService.requestRebind(
                        ComponentName(this@PaymentNotificationListenerService, PaymentNotificationListenerService::class.java),
                    )
                }
                if (attempt < REBIND_ATTEMPTS - 1) delay(REBIND_RETRY_DELAY_MS)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun extractContent(notification: Notification): NotificationContent {
        val extras = notification.extras
        runCatching { extras.classLoader = Bundle::class.java.classLoader }
        val titleParts = linkedSetOf<String>()
        listOf(
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TITLE_BIG,
            Notification.EXTRA_CONVERSATION_TITLE,
        ).forEach { key ->
            extras.getCharSequence(key)?.toString()?.takeIf(String::isNotBlank)?.let(titleParts::add)
        }

        val bodyParts = linkedSetOf<String>()
        listOf(
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_SUMMARY_TEXT,
            Notification.EXTRA_INFO_TEXT,
        ).forEach { key ->
            extras.getCharSequence(key)?.toString()?.takeIf(String::isNotBlank)?.let(bodyParts::add)
        }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
            ?.forEach(bodyParts::add)
        messageBundles(extras).forEach { message ->
            message.getCharSequence("text")?.toString()?.takeIf(String::isNotBlank)?.let(bodyParts::add)
            message.getCharSequence("sender")?.toString()?.takeIf(String::isNotBlank)?.let(bodyParts::add)
        }

        // Bank apps do not all use the standard Notification.EXTRA_* fields. Read only
        // text-shaped extras as a compatibility fallback; icons and other parcelables are ignored.
        extras.keySet().forEach { key ->
            if (key in STANDARD_EXTRA_KEYS) return@forEach
            runCatching { extras.get(key) }.getOrNull()?.let { value ->
                when (value) {
                    is CharSequence -> value.toString().takeIf(String::isNotBlank)?.let(bodyParts::add)
                    is Array<*> -> value.filterIsInstance<CharSequence>()
                        .map(CharSequence::toString)
                        .filter(String::isNotBlank)
                        .forEach(bodyParts::add)
                    is ArrayList<*> -> value.filterIsInstance<CharSequence>()
                        .map(CharSequence::toString)
                        .filter(String::isNotBlank)
                        .forEach(bodyParts::add)
                }
            }
        }
        notification.tickerText?.toString()?.takeIf(String::isNotBlank)?.let(bodyParts::add)

        return NotificationContent(
            title = titleParts.joinToString(" ").normalize().take(200),
            body = bodyParts.joinToString(" ").normalize().take(2_000),
        )
    }

    @Suppress("DEPRECATION")
    private fun messageBundles(extras: Bundle): List<Bundle> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        extras.getParcelableArray(Notification.EXTRA_MESSAGES, Bundle::class.java)?.toList().orEmpty()
    } else {
        extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.mapNotNull { it as? Bundle }.orEmpty()
    }

    private data class NotificationContent(
        val title: String,
        val body: String,
    )

    private companion object {
        const val TAG = "MoasseumNotification"
        const val AI_CLASSIFICATION_TIMEOUT_MS = 4_500L
        const val REBIND_ATTEMPTS = 4
        const val REBIND_RETRY_DELAY_MS = 750L
        val STANDARD_EXTRA_KEYS = setOf(
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TITLE_BIG,
            Notification.EXTRA_CONVERSATION_TITLE,
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_SUMMARY_TEXT,
            Notification.EXTRA_INFO_TEXT,
            Notification.EXTRA_TEXT_LINES,
            Notification.EXTRA_MESSAGES,
        )
    }
}

private fun String.normalize(): String = replace(Regex("\\s+"), " ").trim()
