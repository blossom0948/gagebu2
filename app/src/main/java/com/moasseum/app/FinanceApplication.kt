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
import kotlinx.coroutines.launch

class FinanceApplication : Application() {
    val database by lazy { FinanceDatabase.create(this) }
    val financeRepository by lazy { FinanceRepository(database.financeDao()) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
    val backupManager by lazy { com.moasseum.app.data.BackupManager(this, financeRepository, preferencesRepository) }
    val authRepository by lazy {
        com.moasseum.app.auth.AuthRepository(
            com.moasseum.app.auth.SupabaseAuthApi(), com.moasseum.app.auth.EncryptedSessionStore(this),
            pendingStore = com.moasseum.app.auth.EncryptedPendingAuthStore(this), redirectUri = "$packageName://auth/callback",
        )
    }
    val aiClient by lazy { com.moasseum.app.data.AiClient(bearerTokenProvider = {
        authRepository.validAccessToken() ?: throw java.io.IOException("AI 기능은 관리 → 로그인·계정에서 로그인한 뒤 사용할 수 있어요. 기본 알림 감지와 기기 기록은 계속 동작합니다.")
    }) }
    private val applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    private val notificationCandidateEventsChannel = Channel<NotificationCandidate>(Channel.BUFFERED)
    val notificationCandidateEvents = notificationCandidateEventsChannel.receiveAsFlow()

    @Volatile
    var isActivityVisible: Boolean = false
        private set

    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            authRepository.initialize()
            authRepository.validAccessToken()
        }
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

    fun publishNotificationCandidate(candidate: NotificationCandidate): Boolean =
        notificationCandidateEventsChannel.trySend(candidate).isSuccess
}
