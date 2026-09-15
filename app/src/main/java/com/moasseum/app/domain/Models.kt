package com.moasseum.app.domain

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
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
) {
    val occurredDate: LocalDate
        get() = Instant.ofEpochMilli(occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
}

data class CategoryTotal(
    val key: String,
    val total: Long,
    val count: Int,
)

data class LedgerUiState(
    val month: YearMonth,
    val transactions: List<Transaction> = emptyList(),
    val budgetAmount: Long? = null,
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

    val todayExpenseTotal: Long
        get() = monthTransactions.filter { it.type == TransactionType.EXPENSE && it.occurredDate == LocalDate.now() }.sumOf(Transaction::amount)

    val weekExpenseTotal: Long
        get() {
            val today = LocalDate.now()
            val weekStart = today.with(DayOfWeek.MONDAY)
            return monthTransactions
                .filter { it.type == TransactionType.EXPENSE && it.occurredDate in weekStart..today }
                .sumOf(Transaction::amount)
        }

    val todayTransactionCount: Int
        get() = monthTransactions.count { it.occurredDate == LocalDate.now() }

    val weekTransactionCount: Int
        get() {
            val today = LocalDate.now()
            val weekStart = today.with(DayOfWeek.MONDAY)
            return monthTransactions.count { it.occurredDate in weekStart..today }
        }
}

fun parseAmount(input: String): Long? =
    input
        .filter(Char::isDigit)
        .takeIf(String::isNotEmpty)
        ?.toLongOrNull()
        ?.takeIf { it > 0 }

fun formatWon(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
    return "₩${formatter.format(amount)}"
}

fun formatSignedWon(amount: Long, type: TransactionType): String =
    when (type) {
        TransactionType.EXPENSE -> "−${formatWon(amount)}"
        TransactionType.INCOME -> "+${formatWon(amount)}"
    }

fun formatMonth(month: YearMonth): String = "${month.year}년 ${month.monthValue}월"

fun formatDate(date: LocalDate): String = "${date.monthValue}월 ${date.dayOfMonth}일"

fun formatLongDate(date: LocalDate): String =
    "${date.monthValue}월 ${date.dayOfMonth}일 ${date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.KOREAN)}"
