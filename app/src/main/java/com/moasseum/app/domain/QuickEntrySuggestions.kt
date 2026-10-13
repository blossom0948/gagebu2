package com.moasseum.app.domain

import java.security.MessageDigest
import java.util.Locale

fun quickEntrySuggestionKey(
    type: TransactionType,
    amount: Long,
    merchant: String,
    categoryKey: String,
    paymentMethod: String,
    currency: String = "KRW",
    originalAmount: String? = null,
): String {
    val canonical = listOf(
        type.name, amount.toString(), merchant.trim().lowercase(Locale.ROOT), categoryKey,
        paymentMethod.trim().lowercase(Locale.ROOT), currency, originalAmount.orEmpty(),
    ).joinToString("\u001f")
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

fun quickEntrySuggestions(
    transactions: List<Transaction>,
    hiddenKeys: Set<String> = emptySet(),
    limit: Int = 8,
): List<QuickEntrySuggestion> {
    require(limit in 1..30)
    return transactions.asSequence()
        .filter { it.type !in setOf(TransactionType.TRANSFER, TransactionType.EXTERNAL_DEPOSIT) && it.merchant.isNotBlank() && it.sharingScope != "SHARED" }
        .sortedWith(compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id })
        .map(::toQuickEntrySuggestion)
        .distinctBy(QuickEntrySuggestion::key)
        .filterNot { it.key in hiddenKeys }
        .take(limit)
        .toList()
}

/** Finds earlier personal entries for the merchant currently being typed. */
fun matchingQuickEntrySuggestions(
    transactions: List<Transaction>,
    merchantQuery: String,
    type: TransactionType,
    limit: Int = 3,
): List<QuickEntrySuggestion> {
    require(limit in 1..10)
    val query = merchantQuery.trim().lowercase(Locale.ROOT)
    if (query.length < 2 || type !in setOf(TransactionType.EXPENSE, TransactionType.INCOME)) return emptyList()

    return transactions.asSequence()
        .filter {
            it.type == type && it.type in setOf(TransactionType.EXPENSE, TransactionType.INCOME) &&
                it.merchant.isNotBlank() && it.sharingScope != "SHARED" &&
                it.merchant.trim().lowercase(Locale.ROOT).contains(query)
        }
        .sortedWith(
            compareByDescending<Transaction> { it.merchant.trim().lowercase(Locale.ROOT).startsWith(query) }
                .thenByDescending { it.occurredAt }
                .thenByDescending { it.id },
        )
        .map(::toQuickEntrySuggestion)
        .distinctBy(QuickEntrySuggestion::key)
        .take(limit)
        .toList()
}

private fun toQuickEntrySuggestion(transaction: Transaction): QuickEntrySuggestion {
    val key = quickEntrySuggestionKey(
        transaction.type, transaction.amount, transaction.merchant, transaction.categoryKey,
        transaction.paymentMethod, transaction.currency, transaction.originalAmount,
    )
    return QuickEntrySuggestion(
        key = key,
        merchant = transaction.merchant,
        amount = transaction.amount,
        type = transaction.type,
        categoryKey = transaction.categoryKey,
        paymentMethod = transaction.paymentMethod,
        currency = transaction.currency,
        originalAmount = transaction.originalAmount,
        exchangeRateToKrw = transaction.exchangeRateToKrw,
        exchangeRateDate = transaction.exchangeRateDate,
    )
}
