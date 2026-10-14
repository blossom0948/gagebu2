package com.moasseum.app.ui.theme

import android.os.Build
import android.animation.ValueAnimator
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moasseum.app.domain.AppPalette

val LocalFinanceColors = staticCompositionLocalOf { DarkFinanceColors }

@Immutable
data class FinanceMotionSettings(
    val reduceMotion: Boolean = false,
) {
    val fast: Int get() = if (reduceMotion) 0 else 160
    val standard: Int get() = if (reduceMotion) 0 else 240
    val emphasized: Int get() = if (reduceMotion) 0 else 280
}

val LocalFinanceMotion = staticCompositionLocalOf { FinanceMotionSettings() }

internal fun shouldReduceMotion(userPreference: Boolean, systemAnimationsEnabled: Boolean): Boolean =
    userPreference || !systemAnimationsEnabled

@Composable
fun MoasseumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.MINT,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val systemAnimationsEnabled = remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemAnimationsEnabled.value = ValueAnimator.areAnimatorsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val effectiveReduceMotion = shouldReduceMotion(reduceMotion, systemAnimationsEnabled.value)
    val systemAccent = rememberSystemAccent(palette, darkTheme)
    val financeColors = financeColorsForPalette(palette, darkTheme, systemAccent)
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = financeColors.accent,
            onPrimary = financeColors.onAccent,
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
            outlineVariant = financeColors.divider,
            surfaceTint = financeColors.accent,
            surfaceContainerLowest = financeColors.surfaceBase,
            surfaceContainerLow = financeColors.surfaceRaised,
            surfaceContainer = financeColors.surfaceRaised,
            surfaceContainerHigh = financeColors.surfaceOverlay,
            surfaceContainerHighest = financeColors.surfaceOverlay,
            secondaryContainer = financeColors.accentSoft,
            onSecondaryContainer = financeColors.textPrimary,
            error = financeColors.expense,
        )
    } else {
        lightColorScheme(
            primary = financeColors.accent,
            onPrimary = financeColors.onAccent,
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
            outlineVariant = financeColors.divider,
            surfaceTint = financeColors.accent,
            surfaceContainerLowest = financeColors.surfaceBase,
            surfaceContainerLow = financeColors.surfaceRaised,
            surfaceContainer = financeColors.surfaceRaised,
            surfaceContainerHigh = financeColors.surfaceOverlay,
            surfaceContainerHighest = financeColors.surfaceOverlay,
            secondaryContainer = financeColors.accentSoft,
            onSecondaryContainer = financeColors.textPrimary,
            error = financeColors.expense,
        )
    }

    CompositionLocalProvider(
        LocalFinanceColors provides financeColors,
        LocalFinanceMotion provides FinanceMotionSettings(effectiveReduceMotion),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MoasseumTypography,
            shapes = MoasseumShapes,
            content = content,
        )
    }
}

@Composable
internal fun rememberSystemAccent(palette: AppPalette, darkTheme: Boolean): SystemAccentColors? {
    val context = LocalContext.current
    if (palette != AppPalette.SYSTEM || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return remember(context, darkTheme) {
        val systemScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        SystemAccentColors(
            accent = systemScheme.primary,
            onAccent = systemScheme.onPrimary,
            secondary = systemScheme.secondary,
            tertiary = systemScheme.tertiary,
        )
    }
}

private val MoasseumTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.9).sp,
        fontFeatureSettings = "tnum",
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        fontFeatureSettings = "tnum",
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

private val MoasseumShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)
