package com.moasseum.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class FinanceColors(
    val accent: Color,
    val accentSoft: Color,
    val income: Color,
    val expense: Color,
    val warning: Color,
    val success: Color,
    val surfaceBase: Color,
    val surfaceRaised: Color,
    val surfaceOverlay: Color,
    val surfaceInput: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
)

val DarkFinanceColors = FinanceColors(
    accent = Color(0xFF2EDAD2),
    accentSoft = Color(0xFF1B4844),
    income = Color(0xFF8ED4FF),
    expense = Color(0xFFFFA38F),
    warning = Color(0xFFFFD58A),
    success = Color(0xFF63E6BE),
    surfaceBase = Color(0xFF102729),
    surfaceRaised = Color(0xFF223A3D),
    surfaceOverlay = Color(0xFF2D4548),
    surfaceInput = Color(0xFF193134),
    textPrimary = Color(0xFFE9FBF8),
    textSecondary = Color(0xFFADC3C0),
    divider = Color(0xFF40595A),
)

val LightFinanceColors = FinanceColors(
    accent = Color(0xFF126B5B),
    accentSoft = Color(0xFFD8F4E6),
    income = Color(0xFF1976A8),
    expense = Color(0xFFC75442),
    warning = Color(0xFF9A6700),
    success = Color(0xFF207A57),
    surfaceBase = Color(0xFFEEF4F0),
    surfaceRaised = Color(0xFFFAFCFB),
    surfaceOverlay = Color(0xFFE2EDE7),
    surfaceInput = Color(0xFFF0F5F2),
    textPrimary = Color(0xFF122321),
    textSecondary = Color(0xFF536B64),
    divider = Color(0xFFC9DBD1),
)

val CategoryFood = Color(0xFFFFB86C)
val CategoryTransport = Color(0xFF8FC7FF)
val CategoryShopping = Color(0xFFD6A8FF)
val CategoryLiving = Color(0xFF86D8C1)
val CategoryHealth = Color(0xFFFF9EAD)
val CategoryLeisure = Color(0xFFA8B6FF)
val CategoryOther = Color(0xFFB7C8C4)
