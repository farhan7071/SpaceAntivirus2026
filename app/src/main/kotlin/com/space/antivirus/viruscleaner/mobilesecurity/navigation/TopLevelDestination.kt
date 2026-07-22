package com.space.antivirus.viruscleaner.mobilesecurity.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.space.antivirus.feature.clean.CleanNavigationRoute
import com.space.antivirus.feature.home.HomeNavigationRoute
import com.space.antivirus.feature.security.SecurityCenterNavigationRoute
import com.space.antivirus.feature.settings.SettingsNavigationRoute

/**
 * The exact 4-tab bottom navigation from Sprint 002.5 §5 — Home /
 * Security Center / Clean / Settings. Deliberately only 4 destinations;
 * see Sprint 002 §4 for why the old app's ~15-destination grid was
 * collapsed to this set.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(HomeNavigationRoute, "Home", Icons.Filled.Home),
    SECURITY_CENTER(SecurityCenterNavigationRoute, "Security Center", Icons.Filled.Security),
    CLEAN(CleanNavigationRoute, "Clean", Icons.Filled.CleaningServices),
    SETTINGS(SettingsNavigationRoute, "Settings", Icons.Filled.Settings),
}
