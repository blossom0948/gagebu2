package com.moasseum.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.moasseum.app.FinanceApplication
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class RecurringTransactionWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = runCatching {
        val application = applicationContext as? FinanceApplication
            ?: error("앱 저장소를 준비하지 못했어요.")
        application.financeRepository.postDueRecurringTransactions()
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        private const val UNIQUE_WORK_NAME = "moasseum_recurring_transactions"

        fun schedule(context: Context) {
            val now = ZonedDateTime.now()
            var nextRun = now.withHour(3).withMinute(0).withSecond(0).withNano(0)
            if (!nextRun.isAfter(now)) nextRun = nextRun.plusDays(1)
            val initialDelayMs = Duration.between(now, nextRun).toMillis().coerceAtLeast(0L)
            val request = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
