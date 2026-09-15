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
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
)

val DarkFinanceColors = FinanceColors(
    accent = Color(0xFF2EDAD2),
    accentSoft = Color(0xFF155154),
    income = Color(0xFF8ED4FF),
    expense = Color(0xFFFFA38F),
    warning = Color(0xFFFFD58A),
    success = Color(0xFF63E6BE),
    surfaceBase = Color(0xFF09292B),
    surfaceRaised = Color(0xFF123739),
    surfaceOverlay = Color(0xFF1A484A),
    textPrimary = Color(0xFFE9FBF8),
    textSecondary = Color(0xFFA2C0BE),
    divider = Color(0xFF286064),
)

val LightFinanceColors = FinanceColors(
    accent = Color(0xFF126B5B),
    accentSoft = Color(0xFFD8F4E6),
    income = Color(0xFF1976A8),
    expense = Color(0xFFC75442),
    warning = Color(0xFF9A6700),
    success = Color(0xFF207A57),
    surfaceBase = Color(0xFFF5FBF7),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceOverlay = Color(0xFFE8F3ED),
    textPrimary = Color(0xFF122321),
    textSecondary = Color(0xFF58716A),
    divider = Color(0xFFD3E5DC),
)

val CategoryFood = Color(0xFFFFB86C)
val CategoryTransport = Color(0xFF8FC7FF)
val CategoryShopping = Color(0xFFD6A8FF)
val CategoryLiving = Color(0xFF86D8C1)
val CategoryHealth = Color(0xFFFF9EAD)
val CategoryLeisure = Color(0xFFA8B6FF)
val CategoryOther = Color(0xFFB7C8C4)
