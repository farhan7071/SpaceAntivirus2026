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
 * chosen deliberately, with documented reasoning, in Sprint 002.5 Section 2:
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

// ---------------------------------------------------------------------
// Sprint 045 — the rest of the Material 3 scheme.
//
// Until this sprint only ten roles were assigned. Material 3 defines
// roughly thirty, and every unassigned one silently falls back to the
// Material BASELINE palette, which is purple. That was not a subtle
// problem:
//
//   onSurfaceVariant   -> baseline #49454F. This is the colour of almost
//                         every line of secondary text in the app.
//   surfaceContainer*  -> baseline #F3EDF7 in light. NavigationBar's
//                         container reads from this, so the bottom bar
//                         was a lavender-tinted grey.
//   secondaryContainer -> baseline #E8DEF8. NavigationBar's SELECTED
//                         indicator pill reads from this, so the active
//                         tab wore a lavender capsule under a teal icon.
//
// It also explains why the light theme felt flat next to the dark one.
// In dark mode Material's elevation tint visibly lightens a raised
// surface, so a 2dp card separates from its background for free. In
// light mode that tint is nearly invisible and depth has to come from a
// container colour that differs from the background — which, with the
// surfaceContainer roles unassigned, did not exist. Cards sat on an
// identical-coloured field with only a 2dp shadow to distinguish them.
//
// Values below continue the same M3 tonal palette the ten original roles
// came from (seed #00696B). background and surface deliberately stay
// equal, per M3: separation comes from the container roles, not from
// diverging those two.
// ---------------------------------------------------------------------

val md_theme_light_secondary = Color(0xFF4A6363)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCCE8E7)
val md_theme_light_onSecondaryContainer = Color(0xFF051F1F)
val md_theme_light_tertiary = Color(0xFF4B607C)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFD3E4FF)
val md_theme_light_onTertiaryContainer = Color(0xFF041C35)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_onSurfaceVariant = Color(0xFF3F4948)
val md_theme_light_outlineVariant = Color(0xFFBEC9C8)
val md_theme_light_scrim = Color(0xFF000000)
val md_theme_light_inverseSurface = Color(0xFF2D3131)
val md_theme_light_inverseOnSurface = Color(0xFFEFF1F0)
val md_theme_light_inversePrimary = Color(0xFF4DDADA)
val md_theme_light_surfaceDim = Color(0xFFD9DBDA)
val md_theme_light_surfaceBright = Color(0xFFFAFDFC)
val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceContainerLow = Color(0xFFF4F6F5)
val md_theme_light_surfaceContainer = Color(0xFFEEF1F0)
val md_theme_light_surfaceContainerHigh = Color(0xFFE8EBEA)
val md_theme_light_surfaceContainerHighest = Color(0xFFE3E5E4)

val md_theme_dark_secondary = Color(0xFFB0CCCB)
val md_theme_dark_onSecondary = Color(0xFF1B3534)
val md_theme_dark_secondaryContainer = Color(0xFF324B4B)
val md_theme_dark_onSecondaryContainer = Color(0xFFCCE8E7)
val md_theme_dark_tertiary = Color(0xFFB3C8E8)
val md_theme_dark_onTertiary = Color(0xFF1C314B)
val md_theme_dark_tertiaryContainer = Color(0xFF334863)
val md_theme_dark_onTertiaryContainer = Color(0xFFD3E4FF)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_onSurfaceVariant = Color(0xFFBEC9C8)
val md_theme_dark_outlineVariant = Color(0xFF3F4948)
val md_theme_dark_scrim = Color(0xFF000000)
val md_theme_dark_inverseSurface = Color(0xFFE0E3E2)
val md_theme_dark_inverseOnSurface = Color(0xFF191C1C)
val md_theme_dark_inversePrimary = Color(0xFF00696B)
val md_theme_dark_surfaceDim = Color(0xFF191C1C)
val md_theme_dark_surfaceBright = Color(0xFF363A39)
val md_theme_dark_surfaceContainerLowest = Color(0xFF0B0F0F)
val md_theme_dark_surfaceContainerLow = Color(0xFF191C1C)
val md_theme_dark_surfaceContainer = Color(0xFF1D2020)
val md_theme_dark_surfaceContainerHigh = Color(0xFF272B2B)
val md_theme_dark_surfaceContainerHighest = Color(0xFF323535)
