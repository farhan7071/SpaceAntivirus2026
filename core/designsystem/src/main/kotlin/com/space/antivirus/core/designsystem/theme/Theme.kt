package com.space.antivirus.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    outline = md_theme_light_outline,
    surfaceVariant = md_theme_light_surfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    outline = md_theme_dark_outline,
    surfaceVariant = md_theme_dark_surfaceVariant,
)

/**
 * App-wide theme root.
 *
 * Sprint 035 (SDS v1.0, Part 1 — Brand Identity): dynamicColor now
 * defaults to false, reversing Sprint 002.5 §8's original default. That
 * default meant this app's actual on-screen colors — on more than half
 * the Android install base (API 31+) — were never the deliberately
 * chosen brand teal (Color.kt's own KDoc) at all, but whatever each
 * user's wallpaper happened to generate. A security app's brand
 * identity is exactly the kind of thing this sprint asks to be
 * "professional... trustworthy... premium... memorable" and consistent
 * across users — dynamic, wallpaper-dependent color works directly
 * against every one of those, and against Sprint 002.5 §2's own
 * documented reasoning for choosing a specific, differentiated brand
 * color in the first place. The parameter itself is untouched — a
 * caller can still explicitly opt back into dynamic color by passing
 * `dynamicColor = true` — only the default changed. See ADR 0049.
 */
@Composable
fun SpaceAntivirusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
