package com.moasseum.app

import com.moasseum.app.domain.RecurringRule
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.detectRecurringExpensePatterns
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringExpensePatternTest {
    @Test fun `suggests only stable expense in three consecutive months`() {
        val rows = listOf(1000L to "2026-08-10", 1100L to "2026-09-10", 1050L to "2026-10-10")
            .mapIndexed { index, (amount, date) -> expense(amount, date, index.toLong()) }
        val result = detectRecurringExpensePatterns(rows, emptyList(), YearMonth.parse("2026-10"))
        assertEquals(1, result.size)
        assertEquals("스트리밍", result.single().merchant)
        assertEquals(1_050L, result.single().averageAmount)
        assertEquals(3, result.single().monthsSeen)
    }

    @Test fun `does not suggest missing month unstable amount or active rule`() {
        val rows = listOf(
            expense(1000, "2026-07-10", 1), expense(1000, "2026-09-10", 2), expense(5000, "2026-10-10", 3),
            expense(2000, "2026-08-11", 4, "음악"), expense(2100, "2026-09-11", 5, "음악"), expense(1900, "2026-10-11", 6, "음악"),
        )
        val existing = RecurringRule(1, TransactionType.EXPENSE, 2000, "음악", 11, LocalDate.parse("2026-11-11"), "SUBSCRIPTION", "", "카드", true)
        assertTrue(detectRecurringExpensePatterns(rows, listOf(existing), YearMonth.parse("2026-10")).isEmpty())
    }

    private fun expense(amount: Long, date: String, id: Long, merchant: String = "스트리밍") = Transaction(
        id = id + 1,
        type = TransactionType.EXPENSE,
        amount = amount,
        occurredAt = LocalDate.parse(date).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        categoryKey = "SUBSCRIPTION",
        merchant = merchant,
        memo = "",
        paymentMethod = "카드",
        source = "MANUAL",
        timezone = "UTC",
    )
}
