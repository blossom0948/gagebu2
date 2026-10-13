package com.moasseum.app

import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.quickEntrySuggestionKey
import com.moasseum.app.domain.quickEntrySuggestions
import com.moasseum.app.domain.matchingQuickEntrySuggestions
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

    @Test fun typingMerchantFindsRecentPersonalEntriesForTheSelectedType() {
        val matches = matchingQuickEntrySuggestions(listOf(
            transaction(1, "Cafe Bene", 4500),
            transaction(2, "Cafe Bene", 5000),
            transaction(3, "Cafe Bene", 3000, type = TransactionType.INCOME),
            transaction(4, "Other Cafe", 1200, scope = "SHARED"),
            transaction(5, "Transfer Cafe", 2500, type = TransactionType.TRANSFER),
        ), "cafe", TransactionType.EXPENSE)

        assertEquals(listOf(5000L, 4500L), matches.map { it.amount })
        assertTrue(matchingQuickEntrySuggestions(listOf(transaction(6, "Cafe", 1000)), "c", TransactionType.EXPENSE).isEmpty())
    }

    @Test fun typingMerchantLimitsMatchesAndRejectsTransfers() {
        val transactions = (1L..6L).map { transaction(it, "Market_$it", it * 1000) } +
            transaction(7, "Market Transfer", 1000, type = TransactionType.TRANSFER)

        val matches = matchingQuickEntrySuggestions(transactions, "market", TransactionType.EXPENSE, limit = 2)

        assertEquals(2, matches.size)
        assertEquals(listOf(6000L, 5000L), matches.map { it.amount })
    }

    @Test fun partialKoreanMerchantSearchRanksNamePrefixBeforeNewerIncidentalMatches() {
        val matches = matchingQuickEntrySuggestions(
            listOf(
                transaction(30, "생활비 공동 모임", 2_000),
                transaction(20, "공동 생활비", 50_000, category = "LIVING"),
                transaction(10, "공동 생활비", 40_000, category = "LIVING"),
            ),
            merchantQuery = "공동",
            type = TransactionType.EXPENSE,
        )

        assertEquals(listOf("공동 생활비", "공동 생활비", "생활비 공동 모임"), matches.map { it.merchant })
        assertEquals(listOf(50_000L, 40_000L, 2_000L), matches.map { it.amount })
    }
}
