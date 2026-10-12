package com.moasseum.app

import com.moasseum.app.domain.daysTogetherInclusive
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelationshipCounterTest {
    @Test fun countsStartDateAsDayOneAndAdvancesAcrossLeapDay() {
        assertEquals(1L, daysTogetherInclusive(LocalDate.parse("2024-02-28"), LocalDate.parse("2024-02-28")))
        assertEquals(2L, daysTogetherInclusive(LocalDate.parse("2024-02-28"), LocalDate.parse("2024-02-29")))
        assertEquals(3L, daysTogetherInclusive(LocalDate.parse("2024-02-28"), LocalDate.parse("2024-03-01")))
    }

    @Test fun missingOrFutureStartDateDoesNotShowACounter() {
        val today = LocalDate.parse("2026-10-13")
        assertNull(daysTogetherInclusive(null, today))
        assertNull(daysTogetherInclusive(today.plusDays(1), today))
    }
}
