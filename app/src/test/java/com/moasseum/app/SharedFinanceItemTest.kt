package com.moasseum.app

import com.moasseum.app.data.SharedFinanceItem
import com.moasseum.app.data.SharedFinanceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedFinanceItemTest {
    @Test fun validatesRecurringFinanceAndMonthlyBudgetShapes() {
        val income = item(SharedFinanceKind.MONTHLY_INCOME, amount = 3_000_000, dueDay = 25)
        val budget = item(SharedFinanceKind.LIVING_BUDGET, amount = 1_200_000, monthKey = "2026-10")

        assertEquals(SharedFinanceKind.MONTHLY_INCOME, income.kind)
        assertEquals("2026-10", budget.monthKey)
    }

    @Test fun anniversaryAcceptsLeapDayButRejectsImpossibleDates() {
        val valid = item(SharedFinanceKind.ANNIVERSARY, dateKey = "02-29")
        assertEquals("02-29", valid.dateKey)
        assertTrue(runCatching { item(SharedFinanceKind.ANNIVERSARY, dateKey = "02-31") }.isFailure)
    }

    @Test fun rejectsInvalidMoneyAndMissingBillingDay() {
        assertTrue(runCatching { item(SharedFinanceKind.FIXED_EXPENSE, amount = 0, dueDay = 31) }.isFailure)
        assertTrue(runCatching { item(SharedFinanceKind.ALLOWANCE, amount = 100_000, dueDay = null) }.isFailure)
        assertTrue(runCatching { item(SharedFinanceKind.LIVING_BUDGET, amount = 100_000, monthKey = null) }.isFailure)
    }

    private fun item(
        kind: SharedFinanceKind,
        amount: Long? = null,
        dueDay: Int? = null,
        monthKey: String? = null,
        dateKey: String? = null,
    ) = SharedFinanceItem(
        id = "",
        ownerId = "user",
        kind = kind,
        title = "테스트 항목",
        amount = amount,
        dueDay = dueDay,
        monthKey = monthKey,
        dateKey = dateKey,
    )
}
