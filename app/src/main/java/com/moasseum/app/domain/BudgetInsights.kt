package com.moasseum.app.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class BudgetPlan(val month: YearMonth, val amount: Long, val rollover: Boolean)
data class EffectiveBudget(val base: Long, val carried: Long, val rollover: Boolean) {
    val total: Long get() = Math.addExact(base, carried)
}

data class BudgetCycle(val startDate: LocalDate, val endDate: LocalDate) {
    init { require(!endDate.isBefore(startDate)) }
    val startMonth: YearMonth get() = YearMonth.from(startDate)
    val dayCount: Int get() = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
    val label: String get() = "${startDate.monthValue}월 ${startDate.dayOfMonth}일–${endDate.monthValue}월 ${endDate.dayOfMonth}일"
}

fun budgetCycle(startMonth: YearMonth, payday: Int = 1): BudgetCycle {
    require(payday in 1..31)
    val start = startMonth.atDay(payday.coerceAtMost(startMonth.lengthOfMonth()))
    val nextMonth = startMonth.plusMonths(1)
    val endExclusive = nextMonth.atDay(payday.coerceAtMost(nextMonth.lengthOfMonth()))
    return BudgetCycle(start, endExclusive.minusDays(1))
}

fun budgetCycleForDate(date: LocalDate, payday: Int = 1): BudgetCycle {
    require(payday in 1..31)
    val currentStartDay = payday.coerceAtMost(date.lengthOfMonth())
    val startMonth = if (date.dayOfMonth < currentStartDay) YearMonth.from(date).minusMonths(1) else YearMonth.from(date)
    return budgetCycle(startMonth, payday)
}

fun effectiveBudget(
    month: YearMonth,
    plans: List<BudgetPlan>,
    transactions: List<Transaction>,
    payday: Int = 1,
): EffectiveBudget? {
    require(payday in 1..31)
    var previous: BudgetPlan? = null
    var carry = 0L
    for (plan in plans.filter { it.month <= month }.sortedBy { it.month }) {
        if (previous?.month != plan.month.minusMonths(1) || previous?.rollover != true) carry = 0L
        val effective = EffectiveBudget(plan.amount, carry, plan.rollover)
        if (plan.month == month) return effective
        val cycle = budgetCycle(plan.month, payday)
        val spent = transactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE && it.occurredDate in cycle.startDate..cycle.endDate }
            .sumOf(Transaction::amount)
        carry = (effective.total - spent).coerceAtLeast(0L)
        previous = plan
    }
    return null
}

data class BudgetPace(val remainingDays: Int, val dailyAllowance: Long, val projectedExpense: Long?, val previousSamePeriodExpense: Long)

fun budgetPace(state: LedgerUiState, today: LocalDate = LocalDate.now()): BudgetPace {
    val currentCycle = budgetCycleForDate(today, state.budgetPayday)
    val selectedCycle = if (state.month == YearMonth.from(today)) currentCycle else budgetCycle(state.month, state.budgetPayday)
    val selectedIsCurrent = selectedCycle.startMonth == currentCycle.startMonth
    val selectedIsFuture = selectedCycle.startDate.isAfter(today)
    val selectedIsPast = selectedCycle.endDate.isBefore(today)
    val elapsed = when {
        selectedIsFuture -> 0
        selectedIsPast -> selectedCycle.dayCount
        else -> ChronoUnit.DAYS.between(selectedCycle.startDate, today).toInt() + 1
    }.coerceIn(0, selectedCycle.dayCount)
    val daysRemaining = when {
        selectedIsPast -> 0
        selectedIsFuture -> selectedCycle.dayCount
        else -> (selectedCycle.dayCount - elapsed + 1).coerceAtLeast(0)
    }
    val spent = state.transactions.asSequence()
        .filter { it.type == TransactionType.EXPENSE && it.occurredDate in selectedCycle.startDate..selectedCycle.endDate }
        .filter { selectedIsPast || !it.occurredDate.isAfter(today) }
        .sumOf(Transaction::amount)
    val remaining = ((state.budgetAmount ?: 0) - spent).coerceAtLeast(0)
    val previousCycle = budgetCycle(selectedCycle.startMonth.minusMonths(1), state.budgetPayday)
    val previousThrough = previousCycle.startDate.plusDays((elapsed - 1L).coerceAtLeast(0L))
        .coerceAtMost(previousCycle.endDate)
    val previousSpent = if (elapsed == 0) 0L else state.transactions.asSequence()
        .filter { it.type == TransactionType.EXPENSE && it.occurredDate in previousCycle.startDate..previousThrough }
        .sumOf(Transaction::amount)
    val projected = if (elapsed == 0 || selectedIsFuture) null
    else ceil(spent.toDouble() / elapsed * selectedCycle.dayCount).toLong()
    return BudgetPace(daysRemaining, if (daysRemaining == 0) 0 else remaining / daysRemaining, projected, previousSpent)
}
