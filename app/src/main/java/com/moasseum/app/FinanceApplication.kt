package com.moasseum.app

import android.app.Application
import android.app.Activity
import android.os.Bundle
import com.moasseum.app.data.FinanceRepository
import com.moasseum.app.data.UserPreferencesRepository
import com.moasseum.app.data.local.FinanceDatabase
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.notification.PaymentNotificationNotifier
import com.moasseum.app.work.RecurringTransactionWorker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class FinanceApplication : Application() {
    val database by lazy { FinanceDatabase.create(this) }
    val financeRepository by lazy { FinanceRepository(database.financeDao()) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }

    private val notificationCandidateEventsChannel = Channel<NotificationCandidate>(Channel.BUFFERED)
    val notificationCandidateEvents = notificationCandidateEventsChannel.receiveAsFlow()

    @Volatile
    var isActivityVisible: Boolean = false
        private set

    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        PaymentNotificationNotifier.createChannel(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                isActivityVisible = startedActivities > 0
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                isActivityVisible = startedActivities > 0
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        RecurringTransactionWorker.schedule(this)
    }

    fun publishNotificationCandidate(candidate: NotificationCandidate) {
        notificationCandidateEventsChannel.trySend(candidate)
    }
}
