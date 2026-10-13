package com.moasseum.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceMotionPolicyTest {
    @Test fun `user preference always reduces motion`() {
        assertTrue(shouldReduceMotion(userPreference = true, systemAnimationsEnabled = true))
        assertTrue(shouldReduceMotion(userPreference = true, systemAnimationsEnabled = false))
    }

    @Test fun `disabled Android animator scale reduces app motion`() {
        assertTrue(shouldReduceMotion(userPreference = false, systemAnimationsEnabled = false))
    }

    @Test fun `motion stays enabled only when both app and system allow it`() {
        assertFalse(shouldReduceMotion(userPreference = false, systemAnimationsEnabled = true))
    }
}
