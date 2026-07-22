package com.space.antivirus.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * 8dp base grid per Sprint 002.5 §19. 4dp reserved for tight icon/text
 * pairings only — everything else should use a multiple of 8.
 */
data class Spacing(
    val none: androidx.compose.ui.unit.Dp = 0.dp,
    val tight: androidx.compose.ui.unit.Dp = 4.dp,
    val small: androidx.compose.ui.unit.Dp = 8.dp,
    val medium: androidx.compose.ui.unit.Dp = 16.dp,
    val large: androidx.compose.ui.unit.Dp = 24.dp,
    val extraLarge: androidx.compose.ui.unit.Dp = 32.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
