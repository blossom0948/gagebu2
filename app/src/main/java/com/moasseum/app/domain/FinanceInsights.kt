package com.moasseum.app.domain

import java.time.LocalDate
import java.time.YearMonth

data class CardUsage(
    val card: PaymentCard,
    val dueDate: LocalDate,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val transactions: List<Transaction>,
) {
    val total: Long get() = transactions.sumOf(Transaction::amount)
}

/** A user-configured date range, not a bank statement or installment calculation. */
fun cardUsage(card: PaymentCard, dueMonth: YearMonth, transactions: List<Transaction>): CardUsage {
    require(card.dueDay in 1..31 && card.periodEndDay in 1..31)
    require(card.periodEndMonthsBeforeDue in 0..2)
    val endMonth = dueMonth.minusMonths(card.periodEndMonthsBeforeDue.toLong())
    val end = endMonth.atDay(card.periodEndDay.coerceAtMost(endMonth.lengthOfMonth()))
    val previous = endMonth.minusMonths(1)
    val start = previous.atDay(card.periodEndDay.coerceAtMost(previous.lengthOfMonth())).plusDays(1)
    val due = dueMonth.atDay(card.dueDay.coerceAtMost(dueMonth.lengthOfMonth()))
    require(!end.isAfter(due)) { "이용기간 종료일은 결제일 이후일 수 없어요." }
    return CardUsage(
        card, due, start, end,
        transactions.filter {
            it.type == TransactionType.EXPENSE && it.paymentMethod == card.paymentMethod &&
                it.occurredDate in start..end
        }.sortedWith(compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id }),
    )
}

data class UpcomingFixedExpense(val rule: RecurringRule, val date: LocalDate)

fun upcomingFixedExpenses(
    rules: List<RecurringRule>,
    today: LocalDate,
    days: Int = 30,
): List<UpcomingFixedExpense> {
    require(days in 1..366)
    val lastDay = today.plusDays(days.toLong() - 1)
    return rules.filter { it.isActive && it.type == TransactionType.EXPENSE }.flatMap { rule ->
        // Skip overdue occurrences in this preview; the worker handles catch-up separately.
        val first = maxOf(rule.nextOccurrenceDate, today)
        var occurrence = firstRecurringOccurrence(first, rule.dayOfMonth)
        buildList {
            while (!occurrence.isAfter(lastDay)) {
                add(UpcomingFixedExpense(rule, occurrence))
                occurrence = nextRecurringOccurrence(occurrence, rule.dayOfMonth)
            }
        }
    }.sortedWith(compareBy<UpcomingFixedExpense> { it.date }.thenBy { it.rule.id })
}

/** Never generate a second occurrence in a month which has already been processed. */
fun editedRecurringOccurrence(
    nextOccurrence: LocalDate,
    oldDay: Int,
    newDay: Int,
    today: LocalDate,
): LocalDate = if (oldDay == newDay) nextOccurrence else {
    firstRecurringOccurrence(maxOf(today, YearMonth.from(nextOccurrence).atDay(1)), newDay)
}

enum class HistorySort(val label: String) {
    NEWEST("최신순"), OLDEST("오래된순"), HIGHEST("금액 큰순"), LOWEST("금액 작은순");

    val groupsByDate: Boolean get() = this == NEWEST || this == OLDEST
}

data class HistoryQuery(
    val type: TransactionType? = null,
    val categoryKey: String? = null,
    val date: LocalDate? = null,
    val paymentMethod: String? = null,
    val search: String = "",
    val sort: HistorySort = HistorySort.NEWEST,
)

fun filterHistoryTransactions(transactions: List<Transaction>, query: HistoryQuery): List<Transaction> {
    val search = query.search.trim()
    val newest = compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id }
    val comparator = when (query.sort) {
        HistorySort.NEWEST -> newest
        HistorySort.OLDEST -> compareBy<Transaction> { it.occurredAt }.thenBy { it.id }
        HistorySort.HIGHEST -> compareByDescending<Transaction> { it.amount }.then(newest)
        HistorySort.LOWEST -> compareBy<Transaction> { it.amount }.then(newest)
    }
    return transactions.filter {
        (query.type == null || it.type == query.type) &&
            (query.categoryKey == null || it.categoryKey == query.categoryKey) &&
            (query.date == null || it.occurredDate == query.date) &&
            (query.paymentMethod == null || it.paymentMethod == query.paymentMethod) &&
            (search.isBlank() || it.merchant.contains(search, true) || it.memo.contains(search, true) || it.paymentMethod.contains(search, true))
    }.sortedWith(comparator)
}
