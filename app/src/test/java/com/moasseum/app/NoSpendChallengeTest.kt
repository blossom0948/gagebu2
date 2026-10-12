package com.moasseum.app

import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.NoSpendChallengeSettings
import com.moasseum.app.domain.noSpendChallengeHeadline
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

    @Test
    fun `future challenge date is not described as a spending relapse`() {
        val today = LocalDate.of(2026, 10, 8)
        assertEquals(
            "챌린지는 10월 10일부터 시작해요",
            noSpendChallengeHeadline(emptyList(), NoSpendChallengeSettings(today.plusDays(2), 7), today),
        )
    }

    @Test
    fun `today expense and active streak have distinct accurate headlines`() {
        val today = LocalDate.of(2026, 10, 8)
        assertEquals(
            "오늘 지출이 있어요 · 내일 다시 이어가요",
            noSpendChallengeHeadline(
                listOf(transaction(today, TransactionType.EXPENSE)),
                NoSpendChallengeSettings(today.minusDays(2), 7),
                today,
            ),
        )
        assertEquals(
            "3일째 무지출 기록 중",
            noSpendChallengeHeadline(emptyList(), NoSpendChallengeSettings(today.minusDays(2), 7), today),
        )
    }

    @Test
    fun `challenge goal reached headline uses the saved target`() {
        val today = LocalDate.of(2026, 10, 8)
        assertEquals(
            "목표 달성! 3일 연속 무지출",
            noSpendChallengeHeadline(emptyList(), NoSpendChallengeSettings(today.minusDays(2), 3), today),
        )
    }
}
