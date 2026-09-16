package com.moasseum.app

import com.moasseum.app.domain.firstRecurringOccurrence
import com.moasseum.app.domain.nextRecurringOccurrence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringScheduleTest {
    @Test
    fun `31st schedule clamps to February then returns to 31st`() {
        val february = nextRecurringOccurrence(LocalDate.of(2026, 1, 31), 31)
        assertEquals(LocalDate.of(2026, 2, 28), february)
        assertEquals(LocalDate.of(2026, 3, 31), nextRecurringOccurrence(february, 31))
    }

    @Test
    fun `first schedule selects next month when this month day has passed`() {
        assertEquals(
            LocalDate.of(2026, 10, 1),
            firstRecurringOccurrence(LocalDate.of(2026, 9, 17), 1),
        )
        assertEquals(
            LocalDate.of(2026, 9, 17),
            firstRecurringOccurrence(LocalDate.of(2026, 9, 17), 17),
        )
    }
}
