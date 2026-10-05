package com.moasseum.app.domain

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

data class BudgetPlan(val month: YearMonth, val amount: Long, val rollover: Boolean)
data class EffectiveBudget(val base: Long, val carried: Long, val rollover: Boolean) {
    val total: Long get() = Math.addExact(base, carried)
}

fun effectiveBudget(month: YearMonth, plans: List<BudgetPlan>, transactions: List<Transaction>): EffectiveBudget? {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.groupBy { YearMonth.from(it.occurredDate) }.mapValues { (_, rows) -> rows.sumOf { it.amount } }
    var previous: BudgetPlan? = null
    var carry = 0L
    for (plan in plans.filter { it.month <= month }.sortedBy { it.month }) {
        if (previous?.month != plan.month.minusMonths(1) || previous?.rollover != true) carry = 0L
        val effective = EffectiveBudget(plan.amount, carry, plan.rollover)
        if (plan.month == month) return effective
        carry = (effective.total - (expenses[plan.month] ?: 0L)).coerceAtLeast(0L)
        previous = plan
    }
    return null
}

data class BudgetPace(val remainingDays: Int, val dailyAllowance: Long, val projectedExpense: Long?, val previousSamePeriodExpense: Long)

fun budgetPace(state: LedgerUiState, today: LocalDate = LocalDate.now()): BudgetPace {
    val current = YearMonth.from(today)
    val days = when { state.month < current -> 0; state.month > current -> state.month.lengthOfMonth(); else -> state.month.lengthOfMonth() - today.dayOfMonth + 1 }
    val remaining = ((state.budgetAmount ?: 0) - state.expenseTotal).coerceAtLeast(0)
    val elapsed = when { state.month < current -> state.month.lengthOfMonth(); state.month > current -> 0; else -> today.dayOfMonth }
    val spentToDate = state.monthTransactions.filter { it.type == TransactionType.EXPENSE && (state.month < current || it.occurredDate <= today) }.sumOf { it.amount }
    val previousMonth = state.month.minusMonths(1)
    val sameDay = if (state.month == current) minOf(today.dayOfMonth, previousMonth.lengthOfMonth()) else previousMonth.lengthOfMonth()
    val previousSpent = state.transactions.filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.occurredDate) == previousMonth && it.occurredDate.dayOfMonth <= sameDay }.sumOf { it.amount }
    return BudgetPace(days, if (days == 0) 0 else remaining / days, if (elapsed == 0) null else ceil(spentToDate.toDouble() / elapsed * state.month.lengthOfMonth()).toLong(), previousSpent)
}
