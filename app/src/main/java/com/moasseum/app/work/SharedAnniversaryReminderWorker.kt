package com.moasseum.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.moasseum.app.FinanceApplication
import com.moasseum.app.domain.daysUntilAnniversaryReminder
import com.moasseum.app.notification.PaymentNotificationNotifier
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class SharedAnniversaryReminderWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result = runCatching {
        val app = applicationContext as? FinanceApplication ?: error("앱 저장소를 준비하지 못했어요.")
        if (!app.preferencesRepository.financeRemindersEnabled.first() ||
            !PaymentNotificationNotifier.areFinanceRemindersEnabled(applicationContext)
        ) return@runCatching Result.success()
        val today = LocalDate.now()
        app.preferencesRepository.sharedAnniversaries.first().forEach { anniversary ->
            val days = daysUntilAnniversaryReminder(anniversary.dateKey, today) ?: return@forEach
            val occurrence = today.plusDays(days)
            val key = "shared-anniversary:${anniversary.id}:${occurrence}:${days}"
            if (app.preferencesRepository.claimFinanceReminder(key)) {
                PaymentNotificationNotifier.showFinanceReminder(
                    applicationContext,
                    if (days == 0L) "${anniversary.title} 기념일이에요" else "${anniversary.title} 기념일이 내일이에요",
                    "함께 기념할 시간을 준비해 보세요.",
                    key,
                )
            }
        }
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        private const val UNIQUE_WORK_NAME = "moasseum_shared_anniversary_reminders"

        fun schedule(context: Context) {
            val now = ZonedDateTime.now()
            var next = now.withHour(9).withMinute(0).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<SharedAnniversaryReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMillis().coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
