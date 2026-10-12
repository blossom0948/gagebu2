package com.moasseum.app

import com.moasseum.app.domain.AnnualFinanceSummary
import com.moasseum.app.domain.YearEndTaxProgress
import com.moasseum.app.domain.yearEndTaxProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YearEndTaxProgressTest {
    @Test fun `uses one quarter of salary as the minimum spend threshold`() {
        val progress = yearEndTaxProgress(summary(expenses = 6_000_000), grossSalary = 48_000_000)

        assertEquals(YearEndTaxProgress(48_000_000, 12_000_000, 6_000_000), progress)
        assertEquals(6_000_000L, progress?.remainingToMinimum)
        assertEquals(0.5f, progress?.progress)
    }

    @Test fun `caps visual progress and exposes recorded amount above the threshold`() {
        val progress = yearEndTaxProgress(summary(expenses = 15_000_000), grossSalary = 48_000_000)

        assertEquals(1f, progress?.progress)
        assertEquals(0L, progress?.remainingToMinimum)
        assertEquals(3_000_000L, progress?.recordedOverMinimum)
    }

    @Test fun `requires a plausible positive annual salary`() {
        assertNull(yearEndTaxProgress(summary(0), grossSalary = null))
        assertNull(yearEndTaxProgress(summary(0), grossSalary = 0))
        assertNull(yearEndTaxProgress(summary(0), grossSalary = Long.MAX_VALUE))
    }

    private fun summary(expenses: Long) = AnnualFinanceSummary(
        year = 2026,
        expenseTotal = expenses,
        incomeTotal = 0,
        transactionCount = 0,
        transferCount = 0,
        categories = emptyList(),
        paymentMethods = emptyList(),
        months = emptyList(),
    )
}
