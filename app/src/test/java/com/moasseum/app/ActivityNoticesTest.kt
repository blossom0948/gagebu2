package com.moasseum.app

import com.moasseum.app.domain.ActivityNoticeCategory
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.NoSpendChallengeSettings
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.buildActivityNotices
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityNoticesTest {
    @Test
    fun budgetNoticesAppearAtHalfAndOverBudgetWithStableMonthlyIds() {
        val month = YearMonth.of(2026, 10)
        val first = transaction(1, month.atDay(2), 500)
        val second = transaction(2, month.atDay(4), 550)

        val notices = buildActivityNotices(
            state = LedgerUiState(month, listOf(first, second), budgetAmount = 1_000),
            challenge = NoSpendChallengeSettings(),
            today = month.atDay(5),
        )

        assertEquals(setOf("budget:2026-10:half", "budget:2026-10:over"), notices.map { it.id }.toSet())
        assertEquals(second.occurredAt, notices.first { it.id.endsWith(":over") }.createdAt)
        assertTrue(notices.all { it.category == ActivityNoticeCategory.BUDGET })
    }

    @Test
    fun incomeDoesNotCountTowardBudgetAndUnreachedThresholdsStayHidden() {
        val month = YearMonth.of(2026, 10)
        val income = transaction(1, month.atDay(2), 1_000, TransactionType.INCOME)
        val smallExpense = transaction(2, month.atDay(3), 499)

        val notices = buildActivityNotices(
            state = LedgerUiState(month, listOf(income, smallExpense), budgetAmount = 1_000),
            challenge = NoSpendChallengeSettings(),
            today = month.atDay(4),
        )

        assertTrue(notices.isEmpty())
    }

    @Test
    fun historicalMonthDoesNotCreateCurrentInboxBudgetAlerts() {
        val month = YearMonth.of(2026, 9)
        val notices = buildActivityNotices(
            state = LedgerUiState(month, listOf(transaction(1, month.atDay(20), 2_000)), budgetAmount = 1_000),
            challenge = NoSpendChallengeSettings(),
            today = LocalDate.of(2026, 10, 5),
        )

        assertTrue(notices.isEmpty())
    }

    @Test
    fun completedNoSpendChallengeCreatesAReadableLocalNotice() {
        val today = LocalDate.of(2026, 10, 7)
        val notices = buildActivityNotices(
            state = LedgerUiState(YearMonth.from(today)),
            challenge = NoSpendChallengeSettings(startDate = today.minusDays(6), goalDays = 7),
            today = today,
        )

        assertEquals("challenge:2026-10-01:complete:7", notices.single().id)
        assertEquals(ActivityNoticeCategory.CHALLENGE, notices.single().category)
    }

    private fun transaction(
        id: Long,
        date: LocalDate,
        amount: Long,
        type: TransactionType = TransactionType.EXPENSE,
    ) = Transaction(
        id = id,
        type = type,
        amount = amount,
        occurredAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        categoryKey = "FOOD",
        merchant = "가맹점$id",
        memo = "",
        paymentMethod = "카드",
        source = "MANUAL",
    )
}
