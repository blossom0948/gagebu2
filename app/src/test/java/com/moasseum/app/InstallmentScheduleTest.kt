package com.moasseum.app

import com.moasseum.app.domain.installmentSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InstallmentScheduleTest {
    @Test fun `remainder is distributed without changing purchase total`() {
        val result = installmentSchedule(10_001, 3, LocalDate.parse("2026-10-08"))
        assertEquals(listOf(3_334L, 3_334L, 3_333L), result.map { it.amount })
        assertEquals(10_001L, result.sumOf { it.amount })
    }

    @Test fun `month end dates are clamped independently`() {
        val result = installmentSchedule(6_000, 3, LocalDate.parse("2026-01-31"))
        assertEquals(listOf("2026-01-31", "2026-02-28", "2026-03-31"), result.map { it.date.toString() })
    }

    @Test fun `rejects invalid count and smaller than count total`() {
        assertThrows(IllegalArgumentException::class.java) { installmentSchedule(100, 1, LocalDate.now()) }
        assertThrows(IllegalArgumentException::class.java) { installmentSchedule(2, 3, LocalDate.now()) }
    }
}
