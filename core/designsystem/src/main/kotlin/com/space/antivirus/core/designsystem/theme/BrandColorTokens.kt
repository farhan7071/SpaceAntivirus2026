package com.space.antivirus.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Space Design System v1.0 — Brand Tokens.
 *
 * The raw brand palette: the specific colors that make Space Antivirus
 * look like Space Antivirus, independent of what role (primary, on-
 * primary, background...) M3 assigns them to. Split out from what used
 * to be a single Color.kt specifically so "brand tokens" and "semantic
 * color tokens" (SemanticColorTokens.kt) are two separately readable
 * files, not one file mixing raw brand hex values with the app's
 * meaning-carrying status colors (safe/attention/high-risk) — the
 * separation this sprint's design-token layer explicitly asks for.
 * Theme.kt is the only place these values are actually assigned M3
 * roles (via lightColorScheme/darkColorScheme); nothing else should
 * reference a raw md_theme_* value directly — reach for
 * MaterialTheme.colorScheme instead, which is these same values, already
 * resolved to their semantic M3 role for the current theme.
 *
 * BRAND PRIMARY — deliberately still deep blue-teal, not "Security
 * Green." Sprint 035's own reference mockup names Security Green as
 * the primary color, but this project's brand primary was already
 * chosen deliberately, with documented reasoning, in Sprint 002.5 §2:
 * "deliberately not the red/black 'hacker aesthetic' most competitors
 * in Sprint 001's benchmark use." Every commercial competitor Sprint
 * 035's own brief names as inspiration — Bitdefender, Norton,
 * Malwarebytes — already uses a green-primary or red/green security
 * palette. Sprint 035 also explicitly asks for "an original Space
 * Antivirus identity... do not copy them." Following the mockup's
 * literal green over the project's own, already-differentiated teal
 * would mean copying the exact convention this project deliberately
 * chose not to follow, and would contradict this same sprint's own
 * "do not copy competitors" instruction. Kept as-is; this is a
 * deliberate design judgment call, not an oversight — see ADR 0049 for
 * the full reasoning.
 *
 * "Security Green" itself is not discarded — it is formalized as its
 * own, explicitly named semantic color token (SpaceColors.securityGreen,
 * SemanticColorTokens.kt) below, distinct from the brand primary, used
 * for protected/trusted status specifically.
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
val md_theme_light_outline = Color(0xFF6F7979)
val md_theme_light_surfaceVariant = Color(0xFFDAE5E4)

val md_theme_dark_primary = Color(0xFF4DDADA)
val md_theme_dark_onPrimary = Color(0xFF003737)
val md_theme_dark_primaryContainer = Color(0xFF004F50)
val md_theme_dark_onPrimaryContainer = Color(0xFF6FF6F5)
val md_theme_dark_background = Color(0xFF191C1C)
val md_theme_dark_onBackground = Color(0xFFE0E3E2)
val md_theme_dark_surface = Color(0xFF191C1C)
val md_theme_dark_onSurface = Color(0xFFE0E3E2)
val md_theme_dark_outline = Color(0xFF899393)
val md_theme_dark_surfaceVariant = Color(0xFF3F4948)
