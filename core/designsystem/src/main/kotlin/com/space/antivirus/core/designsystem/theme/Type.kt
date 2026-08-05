package com.space.antivirus.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Space Design System v1.0, Part 3 — Typography.
 *
 * M3 Expressive type scale, used per Sprint 002.5 Section 8's *restrained*
 * guidance: displayLarge is reserved for exactly the two "hero moments"
 * (status headline, scan-complete moment) — it is not used generically.
 *
 * Sprint 035 expanded this from 8 of M3's 15 slots to the full scale,
 * so every screen has a real style to reach for at every level the
 * design system's own hierarchy calls for, rather than reusing
 * titleMedium/bodyMedium for visually distinct roles just because those
 * were the only slots defined. Two names in this sprint's brief —
 * "Caption" and "Metadata" — are not part of M3's own type scale (that
 * vocabulary is from Material 2); they map onto the closest M3 slots
 * rather than getting invented, non-standard slots this module's own
 * type system (a plain M3 Typography instance) has no way to expose
 * beyond the 15 already defined: Caption -> labelSmall (the smallest
 * legible label role — helper text, field hints), Metadata ->
 * bodySmall (secondary, de-emphasized body text — timestamps, counts,
 * package names). Both already exist below; this is a naming
 * convention for callers to follow, not a new style.
 */
val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 45.sp, lineHeight = 52.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)
