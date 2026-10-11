package com.moasseum.app

import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.quickEntrySuggestionKey
import com.moasseum.app.domain.quickEntrySuggestions
import org.junit.Assert.*
import org.junit.Test

class QuickEntrySuggestionsTest {
    private fun transaction(
        id: Long,
        merchant: String,
        amount: Long,
        category: String = "FOOD",
        method: String = "카드",
        type: TransactionType = TransactionType.EXPENSE,
        scope: String = "PRIVATE",
    ) = Transaction(
        id = id, type = type, amount = amount, occurredAt = id,
        categoryKey = category, merchant = merchant, memo = "", paymentMethod = method, source = "MANUAL",
        sharingScope = scope,
    )

    @Test fun suggestionsAreRecentDistinctPersonalTransactions() {
        val suggestions = quickEntrySuggestions(listOf(
            transaction(1, "Cafe", 4500),
            transaction(2, "Cafe", 4500),
            transaction(3, "Cafe", 5000),
            transaction(4, "Transfer", 12000, type = TransactionType.TRANSFER),
            transaction(5, "Shared shop", 1200, scope = "SHARED"),
        ))
        assertEquals(2, suggestions.size)
        assertEquals("Cafe", suggestions.first().merchant)
        assertEquals(5000L, suggestions.first().amount)
    }

    @Test fun removedSuggestionStaysHiddenWithoutDeletingItsTransaction() {
        val item = transaction(10, "Bakery", 3800)
        val key = quickEntrySuggestionKey(item.type, item.amount, item.merchant, item.categoryKey, item.paymentMethod)
        assertTrue(quickEntrySuggestions(listOf(item), hiddenKeys = setOf(key)).isEmpty())
        assertEquals("Bakery", item.merchant)
    }

    @Test fun transactionIdentityKeyIsCaseInsensitiveForMerchantAndMethod() {
        assertEquals(
            quickEntrySuggestionKey(TransactionType.EXPENSE, 1000, "Cafe", "FOOD", "CARD"),
            quickEntrySuggestionKey(TransactionType.EXPENSE, 1000, " cafe ", "FOOD", "card"),
        )
    }
}
