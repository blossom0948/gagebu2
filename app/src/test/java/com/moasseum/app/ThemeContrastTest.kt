package com.moasseum.app

import androidx.compose.ui.graphics.Color
import com.moasseum.app.ui.theme.DarkFinanceColors
import com.moasseum.app.ui.theme.LightFinanceColors
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ThemeContrastTest {
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double = value.toDouble().let { if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return channel(color.red) * 0.2126 + channel(color.green) * 0.7152 + channel(color.blue) * 0.0722
    }
    private fun contrast(a: Color, b: Color): Double {
        val first = luminance(a); val second = luminance(b)
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }

    @Test fun `body and secondary text retain readable contrast on all surfaces`() {
        listOf(DarkFinanceColors, LightFinanceColors).forEach { colors ->
            listOf(colors.surfaceBase, colors.surfaceRaised, colors.surfaceOverlay, colors.surfaceInput, colors.accentSoft).forEach { surface ->
                assertTrue("Primary text must remain readable", contrast(colors.textPrimary, surface) >= 4.5)
                assertTrue("Secondary text must remain readable", contrast(colors.textSecondary, surface) >= 4.5)
            }
        }
    }

    @Test fun `mint buttons have readable theme appropriate labels`() {
        assertTrue(contrast(DarkFinanceColors.accent, Color(0xFF06332B)) >= 4.5)
        assertTrue(contrast(LightFinanceColors.accent, Color.White) >= 4.5)
    }

    @Test fun `raised surfaces are distinct without a harsh contrast jump`() {
        assertTrue(contrast(DarkFinanceColors.surfaceBase, DarkFinanceColors.surfaceRaised) in 1.2..1.8)
        assertTrue(contrast(LightFinanceColors.surfaceBase, LightFinanceColors.surfaceRaised) in 1.03..1.3)
    }
}
