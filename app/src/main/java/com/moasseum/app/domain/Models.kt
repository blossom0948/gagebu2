package com.moasseum.app.domain

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.YearMonth
import java.util.Locale

enum class AiCandidateSource {
    SERVER,
    LOCAL,
}

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
}

object HomeDashboardCards {
    const val TODAY_WEEK = "today_week"
    const val TODAY_INSIGHT = "today_insight"
    const val CATEGORIES = "categories"
    const val RECENT_TRANSACTIONS = "recent_transactions"
    const val NO_SPEND_CHALLENGE = "no_spend_challenge"

    val defaults = setOf(TODAY_WEEK, TODAY_INSIGHT, CATEGORIES, RECENT_TRANSACTIONS, NO_SPEND_CHALLENGE)
}

data class NoSpendChallengeSettings(
    val startDate: LocalDate? = null,
    val goalDays: Int = 7,
) {
    val enabled: Boolean get() = startDate != null
}

fun noSpendStreakDays(
    transactions: List<Transaction>,
    startDate: LocalDate,
    today: LocalDate = LocalDate.now(),
): Int {
    if (startDate.isAfter(today)) return 0
    val spendingDays = transactions.asSequence()
        .filter { it.type == TransactionType.EXPENSE }
        .map { it.occurredDate }
        .filter { !it.isBefore(startDate) && !it.isAfter(today) }
        .toHashSet()
    var day = today
    var streak = 0
    while (!day.isBefore(startDate) && streak < 3660 && day !in spendingDays) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

/** Days to the next valid occurrence of a recurring MM-DD date, including leap-day dates. */
fun daysUntilNextAnniversary(dateKey: String, today: LocalDate = LocalDate.now()): Long? {
    val monthDay = runCatching { MonthDay.parse("--$dateKey") }.getOrNull() ?: return null
    for (year in today.year..(today.year + 8)) {
        if (!monthDay.isValidYear(year)) continue
        val occurrence = runCatching { monthDay.atYear(year) }.getOrNull() ?: continue
        if (!occurrence.isBefore(today)) return ChronoUnit.DAYS.between(today, occurrence)
    }
    return null
}

data class AiTransactionCandidate(
    val type: TransactionType,
    val amount: Long,
    val occurredDate: LocalDate,
    val categoryKey: String,
    val merchant: String,
    val memo: String,
    val source: AiCandidateSource,
    val amountConfidence: Double = 1.0,
    val dateConfidence: Double = 1.0,
    val categoryConfidence: Double = 0.7,
    val needsConfirmation: List<String> = emptyList(),
)

sealed interface AiParseState {
    data object Idle : AiParseState
    data object Loading : AiParseState
    data class Success(val candidate: AiTransactionCandidate) : AiParseState
    data class Error(val message: String) : AiParseState
}

data class Transaction(
    val id: Long,
    val type: TransactionType,
    val amount: Long,
    val occurredAt: Long,
    val categoryKey: String,
    val merchant: String,
    val memo: String,
    val paymentMethod: String,
    val source: String,
    val accountId: String? = null,
    val destinationAccountId: String? = null,
    val timezone: String = ZoneId.systemDefault().id,
    val ownerId: String = "local-user",
    val ledgerId: String = "personal",
    val sharingScope: String = "PRIVATE",
    val cloudId: String? = null,
    val installmentGroupId: String? = null,
    val installmentNumber: Int? = null,
    val installmentCount: Int? = null,
) {
    val occurredDate: LocalDate
        get() = Instant.ofEpochMilli(occurredAt).atZone(ZoneId.of(timezone)).toLocalDate()
}

data class InstallmentPart(val number: Int, val amount: Long, val date: LocalDate)

/** Splits a purchase into equal monthly charges while preserving the exact original total. */
fun installmentSchedule(total: Long, count: Int, firstChargeDate: LocalDate): List<InstallmentPart> {
    require(total in 1..1_000_000_000_000L) { "총액은 1원 이상 입력해 주세요." }
    require(count in 2..60) { "할부 개월은 2~60개월로 설정해 주세요." }
    require(total >= count) { "할부 회차마다 최소 1원 이상이어야 해요." }
    val base = total / count
    val remainder = total % count
    val preferredDay = firstChargeDate.dayOfMonth
    return (1..count).map { number ->
        val month = YearMonth.from(firstChargeDate).plusMonths((number - 1).toLong())
        InstallmentPart(number, base + if (number <= remainder) 1 else 0, month.atDay(preferredDay.coerceAtMost(month.lengthOfMonth())))
    }
}

data class RecurringRule(
    val id: Long,
    val type: TransactionType,
    val amount: Long,
    val merchant: String,
    val dayOfMonth: Int,
    val nextOccurrenceDate: LocalDate,
    val categoryKey: String,
    val memo: String,
    val paymentMethod: String,
    val isActive: Boolean,
)

data class PaymentCard(
    val id: String,
    val name: String,
    val dueDay: Int,
    val paymentMethod: String = name,
    val periodEndDay: Int = 31,
    val periodEndMonthsBeforeDue: Int = 1,
)

data class CategoryTotal(
    val key: String,
    val total: Long,
    val count: Int,
)

data class SpendingAnalysis(
    val summary: String,
    val observations: List<String>,
    val suggestions: List<String>,
)

sealed interface SpendingAnalysisState {
    data object Idle : SpendingAnalysisState
    data object Loading : SpendingAnalysisState
    data class Success(val month: YearMonth, val analysis: SpendingAnalysis) : SpendingAnalysisState
    data class Error(val message: String) : SpendingAnalysisState
}

sealed interface SpendingQuestionState {
    data object Idle : SpendingQuestionState
    data object Loading : SpendingQuestionState
    data class Success(val month: YearMonth, val question: String, val answer: String) : SpendingQuestionState
    data class Error(val message: String) : SpendingQuestionState
}

data class LedgerUiState(
    val month: YearMonth,
    val transactions: List<Transaction> = emptyList(),
    val budgetAmount: Long? = null,
    val baseBudgetAmount: Long? = budgetAmount,
    val carriedBudgetAmount: Long = 0,
    val budgetRollover: Boolean = false,
) {
    val monthTransactions: List<Transaction>
        get() = transactions.filter { it.occurredDate.let { date -> YearMonth.from(date) == month } }

    val expenseTotal: Long
        get() = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf(Transaction::amount)

    val incomeTotal: Long
        get() = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf(Transaction::amount)

    val expenseCount: Int
        get() = monthTransactions.count { it.type == TransactionType.EXPENSE }

    val previousExpenseTotal: Long
        get() {
            val previousMonth = month.minusMonths(1)
            return transactions
                .asSequence()
                .filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.occurredDate) == previousMonth }
                .sumOf(Transaction::amount)
        }

    val categoryTotals: List<CategoryTotal>
        get() = monthTransactions
            .asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy(Transaction::categoryKey)
            .map { (key, values) -> CategoryTotal(key, values.sumOf(Transaction::amount), values.size) }
            .sortedByDescending(CategoryTotal::total)

    val latestTransactions: List<Transaction>
        get() = monthTransactions.sortedWith(
            compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id },
        )

    private val todaySpending: ExpensePeriod
        get() = LocalDate.now().let { expensePeriod(transactions, it, it) }
    private val weekSpending: ExpensePeriod
        get() = LocalDate.now().let { expensePeriod(transactions, it.with(DayOfWeek.MONDAY), it) }
    val todayExpenseTotal: Long get() = todaySpending.amount
    val weekExpenseTotal: Long get() = weekSpending.amount
    val todayTransactionCount: Int get() = todaySpending.count
    val weekTransactionCount: Int get() = weekSpending.count
}

data class ExpensePeriod(val amount: Long, val count: Int)

fun expensePeriod(transactions: List<Transaction>, from: LocalDate, through: LocalDate): ExpensePeriod {
    require(!through.isBefore(from))
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE && it.occurredDate in from..through }
    return ExpensePeriod(expenses.sumOf(Transaction::amount), expenses.size)
}

fun parseAmount(input: String): Long? =
    input
        .takeIf { value -> value.all { it in '0'..'9' || it.isWhitespace() || it in ",₩원" } }
        ?.filter { it in '0'..'9' }
        ?.takeIf(String::isNotEmpty)
        ?.toLongOrNull()
        ?.takeIf { it in 1..1_000_000_000_000L }

fun formatWon(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
    return "₩${formatter.format(amount)}"
}

fun formatSignedWon(amount: Long, type: TransactionType): String =
    when (type) {
        TransactionType.EXPENSE -> "−${formatWon(amount)}"
        TransactionType.INCOME -> "+${formatWon(amount)}"
        TransactionType.TRANSFER -> "↔ ${formatWon(amount)}"
    }

fun formatMonth(month: YearMonth): String = "${month.year}년 ${month.monthValue}월"

fun formatDate(date: LocalDate): String = "${date.monthValue}월 ${date.dayOfMonth}일"

fun formatLongDate(date: LocalDate): String =
    "${date.monthValue}월 ${date.dayOfMonth}일 ${date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.KOREAN)}"
