package com.space.antivirus.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Fixed brand-seed palette. Used (a) to *generate* the dynamic-color scheme
 * on Android 12+, and (b) as the literal fallback scheme pre-Android 12.
 * Deep blue-teal per Sprint 002.5 §2 — deliberately not the red/black
 * "hacker aesthetic" most competitors in Sprint 001's benchmark use.
 */
val BrandSeed = Color(0xFF00696B) // deep blue-teal

val md_theme_light_primary = Color(0xFF00696B)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF6FF6F5)
val md_theme_light_onPrimaryContainer = Color(0xFF002020)
val md_theme_light_background = Color(0xFFFAFDFC)
val md_theme_light_onBackground = Color(0xFF191C1C)
val md_theme_light_surface = Color(0xFFFAFDFC)
val md_theme_light_onSurface = Color(0xFF191C1C)

val md_theme_dark_primary = Color(0xFF4DDADA)
val md_theme_dark_onPrimary = Color(0xFF003737)
val md_theme_dark_primaryContainer = Color(0xFF004F50)
val md_theme_dark_onPrimaryContainer = Color(0xFF6FF6F5)
val md_theme_dark_background = Color(0xFF191C1C)
val md_theme_dark_onBackground = Color(0xFFE0E3E2)
val md_theme_dark_surface = Color(0xFF191C1C)
val md_theme_dark_onSurface = Color(0xFFE0E3E2)

/**
 * Semantic severity colors — mapped onto (not replacing) M3 roles, per
 * Sprint 002.75 §4: severity is communicated by icon + text label first,
 * color second. Only three tiers exist by design (Sprint 002.5 §17).
 */
object SeverityColors {
    val InfoLight = Color(0xFF3D6373)
    val AttentionLight = Color(0xFF7B5800)
    val ActionNeededLight = Color(0xFFBA1A1A)

    val InfoDark = Color(0xFFA6CBE0)
    val AttentionDark = Color(0xFFF6BD3F)
    val ActionNeededDark = Color(0xFFFFB4AB)
}
