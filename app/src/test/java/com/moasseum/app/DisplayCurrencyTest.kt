package com.moasseum.app

import com.moasseum.app.domain.formatDisplayAmount
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DisplayCurrencyTest {
    private val today = LocalDate.of(2026, 10, 13)

    @Test fun convertsStoredWonUsingRecentRateWithoutChangingTheStoredAmount() {
        assertEquals("USD 1,000.00", formatDisplayAmount(1_350_500, "USD", "1350.5", "2026-10-12", today))
        assertEquals("₩1,350,500", formatDisplayAmount(1_350_500, "KRW", "1350.5", "2026-10-12", today))
    }

    @Test fun usesCurrencyPrecisionAndFallsBackToWonWhenQuoteIsUnavailableOrStale() {
        assertEquals("JPY 1,001", formatDisplayAmount(1_351, "JPY", "1.35", "2026-10-12", today))
        assertEquals("₩1,000", formatDisplayAmount(1_000, "USD", null, null, today))
        assertEquals("₩1,000", formatDisplayAmount(1_000, "USD", "1350", "2026-09-30", today))
        assertEquals("₩1,000", formatDisplayAmount(1_000, "BAD", "1", "2026-10-12", today))
    }
}
