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
        .map { transaction ->
            val key = quickEntrySuggestionKey(
                transaction.type, transaction.amount, transaction.merchant, transaction.categoryKey,
                transaction.paymentMethod, transaction.currency, transaction.originalAmount,
            )
            QuickEntrySuggestion(
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
        .distinctBy(QuickEntrySuggestion::key)
        .filterNot { it.key in hiddenKeys }
        .take(limit)
        .toList()
}
