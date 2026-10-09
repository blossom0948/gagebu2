package com.moasseum.app

import com.moasseum.app.domain.parseAmount
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ModelsTest {
    @Test
    fun `amount parser removes currency punctuation and spaces`() {
        assertEquals(24000L, parseAmount("₩24,000"))
        assertEquals(8000L, parseAmount("8 000원"))
    }

    @Test
    fun `amount parser rejects zero empty and non numeric values`() {
        assertNull(parseAmount("0"))
        assertNull(parseAmount("   "))
        assertNull(parseAmount("원"))
    }

    @Test
    fun `recent monthly summaries are chronological and expose only grouped totals`() {
        fun row(id: Long, date: String, type: TransactionType, amount: Long, category: String) = Transaction(
            id = id,
            type = type,
            amount = amount,
            occurredAt = LocalDate.parse(date).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
            categoryKey = category,
            merchant = "private merchant $id",
            memo = "private memo",
            paymentMethod = "카드",
            source = "MANUAL",
            timezone = "UTC",
        )
        val state = LedgerUiState(
            month = YearMonth.of(2026, 10),
            transactions = listOf(
                row(1, "2026-08-04", TransactionType.EXPENSE, 20_000, "FOOD"),
                row(2, "2026-09-09", TransactionType.EXPENSE, 5_000, "CAFE"),
                row(3, "2026-09-10", TransactionType.INCOME, 100_000, "OTHER"),
                row(4, "2026-10-01", TransactionType.TRANSFER, 30_000, "OTHER"),
            ),
        )

        val summaries = state.recentMonthlySummaries(4)
        assertEquals(listOf("2026-07", "2026-08", "2026-09", "2026-10"), summaries.map { it.month.toString() })
        assertEquals(20_000L, summaries[1].expenseTotal)
        assertEquals(5_000L, summaries[2].expenseTotal)
        assertEquals(100_000L, summaries[2].incomeTotal)
        assertEquals(0L, summaries[3].expenseTotal)
        assertEquals("CAFE", summaries[2].categories.single().key)
        assertEquals(0, summaries[3].expenseCount)
    }
}
