package com.moasseum.app

import com.moasseum.app.domain.HomeDashboardCards
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDashboardCardsTest {
    @Test
    fun `default home hides the insight that repeats the daily total but keeps it selectable`() {
        assertTrue(HomeDashboardCards.TODAY_WEEK in HomeDashboardCards.defaults)
        assertFalse(HomeDashboardCards.TODAY_INSIGHT in HomeDashboardCards.defaults)
        assertTrue(HomeDashboardCards.TODAY_INSIGHT in HomeDashboardCards.available)
    }
}
