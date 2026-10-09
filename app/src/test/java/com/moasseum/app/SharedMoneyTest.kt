package com.moasseum.app

import com.moasseum.app.domain.SharedMoneyMode
import com.moasseum.app.domain.SharedSavingsGoalAmount
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.sharedPayerTotals
import com.moasseum.app.domain.sharedSavingsProgress
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SharedMoneyTest {
    @Test
    fun `equal settlement uses only shared transactions and identifies payer direction`() {
        val date = LocalDate.of(2026, 10, 9).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val rows = listOf(
            row(1, "me", TransactionType.EXPENSE, 10_000, date),
            row(2, "partner", TransactionType.EXPENSE, 30_000, date),
            row(3, "me", TransactionType.INCOME, 50_000, date),
        )

        val totals = sharedPayerTotals(rows, "me")

        assertEquals(10_000L, totals.myExpense)
        assertEquals(30_000L, totals.partnerExpense)
        assertEquals(50_000L, totals.incomeTotal)
        assertEquals(10_000L, totals.iOwePartner)
        assertEquals(0L, totals.partnerOwesMe)
        assertEquals(SharedMoneyMode.EQUAL_SPLIT, SharedMoneyMode.valueOf("EQUAL_SPLIT"))
    }

    @Test
    fun `no settlement is due when paid amounts are already even`() {
        val date = LocalDate.of(2026, 10, 9).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val totals = sharedPayerTotals(
            listOf(
                row(1, "me", TransactionType.EXPENSE, 12_000, date),
                row(2, "partner", TransactionType.EXPENSE, 12_000, date),
            ),
            "me",
        )
        assertEquals(0L, totals.iOwePartner)
        assertEquals(0L, totals.partnerOwesMe)
    }

    @Test
    fun `monthly savings mode combines this months goal progress`() {
        val progress = sharedSavingsProgress(
            listOf(
                SharedSavingsGoalAmount(target = 1_000_000, current = 250_000),
                SharedSavingsGoalAmount(target = 500_000, current = 500_000),
            ),
        )

        assertEquals(1_500_000L, progress.target)
        assertEquals(750_000L, progress.current)
        assertEquals(0.5, progress.fraction.toDouble(), 0.0001)
        assertEquals(SharedMoneyMode.MONTHLY_SAVINGS, SharedMoneyMode.valueOf("MONTHLY_SAVINGS"))
    }

    @Test
    fun `monthly savings progress handles empty and invalid goal values`() {
        val empty = sharedSavingsProgress(emptyList())
        val invalid = sharedSavingsProgress(listOf(SharedSavingsGoalAmount(target = -5, current = 20)))

        assertEquals(0L, empty.target)
        assertEquals(0f, empty.fraction)
        assertEquals(0L, invalid.target)
        assertEquals(0L, invalid.current)
    }

    private fun row(id: Long, owner: String, type: TransactionType, amount: Long, at: Long) = Transaction(
        id = id,
        ownerId = owner,
        type = type,
        amount = amount,
        occurredAt = at,
        categoryKey = "OTHER",
        merchant = "테스트",
        memo = "",
        paymentMethod = "카드",
        source = "TEST",
    )
}
