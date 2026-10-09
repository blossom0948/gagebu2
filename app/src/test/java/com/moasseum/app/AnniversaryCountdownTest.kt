package com.moasseum.app

import com.moasseum.app.domain.daysUntilNextAnniversary
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnniversaryCountdownTest {
    @Test fun anniversaryShowsTodayAsZeroAndUsesNextYearAfterDatePasses() {
        assertEquals(0L, daysUntilNextAnniversary("10-09", LocalDate.of(2026, 10, 9)))
        assertEquals(1L, daysUntilNextAnniversary("10-10", LocalDate.of(2026, 10, 9)))
        assertEquals(364L, daysUntilNextAnniversary("10-09", LocalDate.of(2026, 10, 10)))
    }

    @Test fun leapDayWaitsForNextLeapYear() {
        assertEquals(1_095L, daysUntilNextAnniversary("02-29", LocalDate.of(2025, 3, 1)))
        assertNull(daysUntilNextAnniversary("not-a-date", LocalDate.of(2026, 10, 9)))
    }
}
