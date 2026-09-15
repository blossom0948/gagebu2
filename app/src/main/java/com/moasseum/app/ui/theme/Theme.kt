package com.moasseum.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalFinanceColors = staticCompositionLocalOf { DarkFinanceColors }

@Immutable
data class FinanceMotionSettings(
    val reduceMotion: Boolean = false,
) {
    val fast: Int get() = if (reduceMotion) 100 else 160
    val standard: Int get() = if (reduceMotion) 120 else 240
    val emphasized: Int get() = if (reduceMotion) 140 else 360
}

val LocalFinanceMotion = staticCompositionLocalOf { FinanceMotionSettings() }

@Composable
fun MoasseumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val financeColors = if (darkTheme) DarkFinanceColors else LightFinanceColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = financeColors.accent,
            onPrimary = Color(0xFF06332B),
            primaryContainer = financeColors.accentSoft,
            onPrimaryContainer = financeColors.textPrimary,
            secondary = financeColors.income,
            background = financeColors.surfaceBase,
            onBackground = financeColors.textPrimary,
            surface = financeColors.surfaceBase,
            onSurface = financeColors.textPrimary,
            surfaceVariant = financeColors.surfaceRaised,
            onSurfaceVariant = financeColors.textSecondary,
            outline = financeColors.divider,
        )
    } else {
        lightColorScheme(
            primary = financeColors.accent,
            onPrimary = Color.White,
            primaryContainer = financeColors.accentSoft,
            onPrimaryContainer = financeColors.textPrimary,
            secondary = financeColors.income,
            background = financeColors.surfaceBase,
            onBackground = financeColors.textPrimary,
            surface = financeColors.surfaceBase,
            onSurface = financeColors.textPrimary,
            surfaceVariant = financeColors.surfaceRaised,
            onSurfaceVariant = financeColors.textSecondary,
            outline = financeColors.divider,
        )
    }

    CompositionLocalProvider(
        LocalFinanceColors provides financeColors,
        LocalFinanceMotion provides FinanceMotionSettings(reduceMotion),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MoasseumTypography,
            shapes = MoasseumShapes,
            content = content,
        )
    }
}

private val MoasseumTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp,
        fontFeatureSettings = "tnum",
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = "tnum",
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

private val MoasseumShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)
