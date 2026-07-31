package com.space.antivirus.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Space Design System v1.0, Part 4 — Spacing.
 *
 * 8dp base grid per Sprint 002.5 §19. 4dp reserved for tight icon/text
 * pairings only — everything else should use a multiple of 8.
 *
 * Sprint 035 added `standard` (12dp), `extraLarge2` (40dp), and
 * `huge` (48dp) — the remaining three values from this sprint's own
 * requested grid (4/8/12/16/24/32/40/48) that the existing six-value
 * scale didn't yet name. Purely additive — every existing field keeps
 * its exact name and value, so no existing call site changes meaning;
 * this only gives future call sites three more named points on the
 * same grid instead of an unnamed literal Dp value, which is exactly
 * what "no random padding" (this sprint's own Part 4 goal) asks for.
 */
data class Spacing(
    val none: androidx.compose.ui.unit.Dp = 0.dp,
    val tight: androidx.compose.ui.unit.Dp = 4.dp,
    val small: androidx.compose.ui.unit.Dp = 8.dp,
    val standard: androidx.compose.ui.unit.Dp = 12.dp,
    val medium: androidx.compose.ui.unit.Dp = 16.dp,
    val large: androidx.compose.ui.unit.Dp = 24.dp,
    val extraLarge: androidx.compose.ui.unit.Dp = 32.dp,
    val extraLarge2: androidx.compose.ui.unit.Dp = 40.dp,
    val huge: androidx.compose.ui.unit.Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
