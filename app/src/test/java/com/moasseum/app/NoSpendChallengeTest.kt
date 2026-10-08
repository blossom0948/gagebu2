package com.moasseum.app

import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.noSpendStreakDays
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class NoSpendChallengeTest {
    private fun transaction(date: LocalDate, type: TransactionType) = Transaction(
        id = date.toEpochDay(),
        type = type,
        amount = 1000,
        occurredAt = date.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
        categoryKey = "OTHER",
        merchant = "테스트",
        memo = "",
        paymentMethod = "현금",
        source = "test",
        timezone = "Asia/Seoul",
    )

    @Test
    fun `counts consecutive days from today and ignores income and transfers`() {
        val today = LocalDate.of(2026, 10, 8)
        val entries = listOf(
            transaction(today, TransactionType.INCOME),
            transaction(today.minusDays(1), TransactionType.TRANSFER),
        )

        assertEquals(3, noSpendStreakDays(entries, today.minusDays(2), today))
    }

    @Test
    fun `spending today resets streak and older spending ends it at the next day`() {
        val today = LocalDate.of(2026, 10, 8)
        assertEquals(0, noSpendStreakDays(listOf(transaction(today, TransactionType.EXPENSE)), today.minusDays(4), today))
        assertEquals(2, noSpendStreakDays(listOf(transaction(today.minusDays(2), TransactionType.EXPENSE)), today.minusDays(4), today))
    }

    @Test
    fun `does not count days before challenge started`() {
        val today = LocalDate.of(2026, 10, 8)
        assertEquals(1, noSpendStreakDays(emptyList(), today, today))
        assertEquals(0, noSpendStreakDays(emptyList(), today.plusDays(1), today))
    }
}
