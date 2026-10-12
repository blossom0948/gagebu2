package com.moasseum.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.moasseum.app.FinanceApplication
import com.moasseum.app.domain.BudgetPlan
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.effectiveBudget
import com.moasseum.app.notification.PaymentNotificationNotifier
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class FinanceReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = runCatching {
        val app = applicationContext as FinanceApplication
        if (!app.preferencesRepository.financeRemindersEnabled.first()) return Result.success()
        if (!PaymentNotificationNotifier.areFinanceRemindersEnabled(applicationContext)) return Result.success()
        val today = LocalDate.now()
        val payday = app.preferencesRepository.budgetPayday.first()
        val budgetCycle = com.moasseum.app.domain.budgetCycleForDate(today, payday)
        val budgetMonth = budgetCycle.startMonth
        val activeTransactions = app.database.financeDao().allTransactionsForBackup().filter { it.deletedAt == null && it.ledgerId == "personal" }
            .map { entity ->
                com.moasseum.app.domain.Transaction(
                    id = entity.id,
                    type = TransactionType.valueOf(entity.type),
                    amount = entity.amount,
                    occurredAt = entity.occurredAt,
                    categoryKey = entity.categoryKey,
                    merchant = entity.merchant,
                    memo = entity.memo,
                    paymentMethod = entity.paymentMethod,
                    source = entity.source,
                    timezone = entity.timezone,
                )
            }
        val plans = app.database.financeDao().allBudgetsForBackup().mapNotNull { row ->
            runCatching { BudgetPlan(YearMonth.parse(row.monthKey), row.amount, row.rollover) }.getOrNull()
        }
        val budget = effectiveBudget(budgetMonth, plans, activeTransactions, payday)?.total
        val spent = activeTransactions.filter {
            it.type == TransactionType.EXPENSE && it.occurredDate in budgetCycle.startDate..minOf(today, budgetCycle.endDate)
        }.sumOf { it.amount }
        if (budget != null && budget > 0L) {
            for (threshold in listOf(50, 100)) {
                val boundary = if (threshold == 50) (budget + 1) / 2 else budget
                if (spent >= boundary) {
                    val key = "budget:$budgetMonth:$threshold"
                    if (app.preferencesRepository.claimFinanceReminder(key)) {
                        PaymentNotificationNotifier.showFinanceReminder(
                            applicationContext,
                            if (threshold == 50) "예산 기간의 절반을 썼어요" else "예산 기간 목표를 넘었어요",
                            "${budgetCycle.label} · ${com.moasseum.app.domain.formatWon(spent)} 사용 · 목표 ${com.moasseum.app.domain.formatWon(budget)}",
                            key,
                        )
                    }
                }
            }
        }

        val tomorrow = today.plusDays(1).toString()
        app.database.financeDao().getDueRecurringRules(tomorrow)
            .filter { it.isActive && it.nextOccurrenceDate == tomorrow && it.type == TransactionType.EXPENSE.name }
            .forEach { rule ->
                val key = "recurring:${rule.id}:$tomorrow"
                if (app.preferencesRepository.claimFinanceReminder(key)) {
                    PaymentNotificationNotifier.showFinanceReminder(
                        applicationContext,
                        "내일 고정 결제가 예정돼 있어요",
                        "${rule.merchant} · ${com.moasseum.app.domain.formatWon(rule.amount)}",
                        key,
                    )
                }
            }
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        private const val UNIQUE_WORK_NAME = "moasseum_finance_reminders"

        fun schedule(context: Context) {
            val now = ZonedDateTime.now()
            var next = now.withHour(9).withMinute(0).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<FinanceReminderWorker>(1, TimeUnit.DAYS)
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
