package com.moasseum.app.notification

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.moasseum.app.FinanceApplication
import com.moasseum.app.data.AiClient
import com.moasseum.app.domain.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val aiClient by lazy { AiClient() }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras
        val title = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString(),
        ).distinct().joinToString(" ").take(200)
        val bodyParts = linkedSetOf<String>()
        listOf(
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_SUMMARY_TEXT,
            Notification.EXTRA_INFO_TEXT,
        ).forEach { key -> extras.getCharSequence(key)?.toString()?.takeIf(String::isNotBlank)?.let(bodyParts::add) }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
            ?.forEach(bodyParts::add)
        val body = bodyParts.joinToString(" ").take(2_000)
        if (!PaymentNotificationParser.shouldInspectWithAi(title, body)) return

        serviceScope.launch {
            val application = application as? FinanceApplication ?: return@launch
            val aiEnabled = application.preferencesRepository.aiNotificationClassificationEnabled.first()
            val classification = if (aiEnabled) {
                aiClient.classifyNotification(title.ifBlank { "알림" }, body).getOrNull()
            } else null
            if (classification != null && !classification.isFinancialTransaction) return@launch
            // If the AI endpoint is unavailable, keep only the strict local parser path below.
            val aiConfirmedType = classification?.transactionType
            val candidate = PaymentNotificationParser.parse(
                packageName = sbn.packageName,
                title = title,
                body = body,
                postedAt = sbn.postTime,
                aiConfirmedType = aiConfirmedType,
            ) ?: return@launch
            val id = application.financeRepository.saveNotificationCandidate(candidate) ?: return@launch
            val detected = candidate.copy(id = id).toDomain()
            if (application.isActivityVisible) {
                application.publishNotificationCandidate(detected)
            } else {
                PaymentNotificationNotifier.showCandidate(this@PaymentNotificationListenerService, detected)
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationListenerService.requestRebind(
            ComponentName(this, PaymentNotificationListenerService::class.java),
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
