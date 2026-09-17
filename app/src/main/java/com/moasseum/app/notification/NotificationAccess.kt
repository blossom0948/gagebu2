package com.moasseum.app.notification

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.provider.Settings
import kotlinx.coroutines.delay

object NotificationAccess {
    private fun listenerComponent(context: Context): ComponentName =
        ComponentName(context, PaymentNotificationListenerService::class.java)

    fun isEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        val expectedComponent = listenerComponent(context)
        return enabledListeners
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { component -> component == expectedComponent }
    }

    fun openSettings(context: Context) {
        val settingsContext = context.applicationContext
        val detailIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    listenerComponent(settingsContext),
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
        val listIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            settingsContext.startActivity(detailIntent ?: listIntent)
        } catch (_: ActivityNotFoundException) {
            settingsContext.startActivity(listIntent)
        } catch (_: SecurityException) {
            settingsContext.startActivity(listIntent)
        }
    }

    fun areAppNotificationsEnabled(context: Context): Boolean =
        PaymentNotificationNotifier.areAppNotificationsEnabled(context)

    fun requestRebind(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isEnabled(context)) return
        runCatching {
            NotificationListenerService.requestRebind(
                listenerComponent(context),
            )
        }
    }

    /**
     * Galaxy/One UI may finish the special-access toggle before the system has
     * recreated the listener binding. A few bounded requests make returning to
     * the app reliable without keeping a background loop alive.
     */
    suspend fun requestRebindWithRetry(context: Context) {
        repeat(REBIND_ATTEMPTS) { attempt ->
            requestRebind(context)
            if (attempt < REBIND_ATTEMPTS - 1) delay(REBIND_RETRY_DELAY_MS)
        }
    }

    fun openAppNotificationSettings(context: Context) {
        val appSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(appSettings)
        } catch (_: ActivityNotFoundException) {
            val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(details)
        }
    }

    private const val REBIND_ATTEMPTS = 4
    private const val REBIND_RETRY_DELAY_MS = 750L
}
