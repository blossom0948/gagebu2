package com.moasseum.app.domain

import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

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

data class AnnualMonthSummary(
    val month: Int,
    val expenseTotal: Long,
    val incomeTotal: Long,
    val transactionCount: Int,
)

data class AnnualPaymentMethodTotal(val paymentMethod: String, val total: Long, val count: Int)

data class AnnualFinanceSummary(
    val year: Int,
    val expenseTotal: Long,
    val incomeTotal: Long,
    val transactionCount: Int,
    val transferCount: Int,
    val categories: List<CategoryTotal>,
    val paymentMethods: List<AnnualPaymentMethodTotal>,
    val months: List<AnnualMonthSummary>,
)

/** A year-end record organizer; this is not a tax deduction or refund estimate. */
fun annualFinanceSummary(transactions: List<Transaction>, year: Int): AnnualFinanceSummary {
    require(year in 1900..9999)
    val yearRows = transactions.filter { it.occurredDate.year == year }
    val expenses = yearRows.filter { it.type == TransactionType.EXPENSE }
    val incomes = yearRows.filter { it.type == TransactionType.INCOME }
    val months = (1..12).map { month ->
        val rows = yearRows.filter { it.occurredDate.monthValue == month }
        val monthExpenses = rows.filter { it.type == TransactionType.EXPENSE }
        val monthIncomes = rows.filter { it.type == TransactionType.INCOME }
        AnnualMonthSummary(
            month = month,
            expenseTotal = monthExpenses.sumOf(Transaction::amount),
            incomeTotal = monthIncomes.sumOf(Transaction::amount),
            transactionCount = monthExpenses.size + monthIncomes.size,
        )
    }
    return AnnualFinanceSummary(
        year = year,
        expenseTotal = expenses.sumOf(Transaction::amount),
        incomeTotal = incomes.sumOf(Transaction::amount),
        transactionCount = expenses.size + incomes.size,
        transferCount = yearRows.count { it.type == TransactionType.TRANSFER },
        categories = expenses.groupBy(Transaction::categoryKey)
            .map { (key, rows) -> CategoryTotal(key, rows.sumOf(Transaction::amount), rows.size) }
            .sortedByDescending(CategoryTotal::total),
        paymentMethods = expenses.groupBy { it.paymentMethod.ifBlank { "미분류" } }
            .map { (method, rows) -> AnnualPaymentMethodTotal(method, rows.sumOf(Transaction::amount), rows.size) }
            .sortedByDescending(AnnualPaymentMethodTotal::total),
        months = months,
    )
}

data class RecurringExpensePattern(
    val merchant: String,
    val averageAmount: Long,
    val categoryKey: String,
    val paymentMethod: String,
    val usualDay: Int,
    val monthsSeen: Int,
)

/** Suggests, but never auto-creates, likely monthly expenses from at least three consecutive months. */
fun detectRecurringExpensePatterns(
    transactions: List<Transaction>,
    existingRules: List<RecurringRule>,
    throughMonth: YearMonth,
    monthWindow: Int = 6,
): List<RecurringExpensePattern> {
    require(monthWindow in 3..24)
    val firstMonth = throughMonth.minusMonths((monthWindow - 1).toLong())
    val known = existingRules.filter { it.isActive && it.type == TransactionType.EXPENSE }
        .mapTo(mutableSetOf()) { normalizedMerchant(it.merchant) }
    return transactions.asSequence()
        .filter { it.type == TransactionType.EXPENSE && it.source !in setOf("INSTALLMENT", "RECURRING") }
        .filter { YearMonth.from(it.occurredDate) in firstMonth..throughMonth }
        .groupBy { normalizedMerchant(it.merchant) }
        .filter { (merchantKey, rows) -> merchantKey.length >= 3 && merchantKey !in known && rows.map { YearMonth.from(it.occurredDate) }.distinct().size >= 3 }
        .mapNotNull { (_, rows) ->
            val perMonth = rows.groupBy { YearMonth.from(it.occurredDate) }
                .mapValues { (_, monthRows) -> monthRows.minByOrNull { it.occurredAt }!! }
                .toSortedMap()
            val lastThree = (0L..2L).map { throughMonth.minusMonths(it) }
            val seenConsecutive = lastThree.all(perMonth::containsKey)
            if (!seenConsecutive) return@mapNotNull null
            val samples = lastThree.map(perMonth::getValue)
            val amounts = samples.map { it.amount }
            val average = amounts.sum() / amounts.size
            if (average <= 0 || amounts.any { it < average * 0.8 || it > average * 1.2 }) return@mapNotNull null
            val displayName = rows.firstOrNull()?.merchant?.take(80) ?: return@mapNotNull null
            RecurringExpensePattern(
                merchant = displayName,
                averageAmount = average,
                categoryKey = samples.groupingBy { it.categoryKey }.eachCount().maxByOrNull { it.value }?.key ?: "OTHER",
                paymentMethod = samples.groupingBy { it.paymentMethod }.eachCount().maxByOrNull { it.value }?.key ?: "카드",
                usualDay = samples.map { it.occurredDate.dayOfMonth }.sorted()[1],
                monthsSeen = perMonth.size,
            )
        }
        .sortedByDescending { it.averageAmount }
}

private fun normalizedMerchant(value: String): String = value.lowercase(Locale.ROOT).filter(Char::isLetterOrDigit)

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
