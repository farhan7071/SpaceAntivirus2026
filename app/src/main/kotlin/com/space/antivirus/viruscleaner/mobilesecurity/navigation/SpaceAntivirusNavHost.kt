package com.space.antivirus.viruscleaner.mobilesecurity.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.space.antivirus.feature.clean.CleanNavigationRoute
import com.space.antivirus.feature.clean.CleanRoute
import com.space.antivirus.feature.history.HistoryNavigationRoute
import com.space.antivirus.feature.history.HistoryRoute
import com.space.antivirus.feature.home.HomeNavigationRoute
import com.space.antivirus.feature.home.HomeRoute
import com.space.antivirus.feature.notifications.NotificationsNavigationRoute
import com.space.antivirus.feature.notifications.NotificationsRoute
import com.space.antivirus.feature.onboarding.OnboardingNavigationRoute
import com.space.antivirus.feature.onboarding.OnboardingRoute
import com.space.antivirus.feature.premium.PremiumNavigationRoute
import com.space.antivirus.feature.premium.PremiumRoute
import com.space.antivirus.feature.realtime.RealTimeNavigationRoute
import com.space.antivirus.feature.realtime.RealTimeRoute
import com.space.antivirus.feature.security.SecurityCenterNavigationRoute
import com.space.antivirus.feature.security.SecurityCenterRoute
import com.space.antivirus.feature.settings.SettingsNavigationRoute
import com.space.antivirus.feature.settings.SettingsRoute

/**
 * The full navigation skeleton required by Sprint 003 Task 4: Home,
 * Security Center, Clean, Settings, Onboarding, Premium, History,
 * Permissions (folded into Settings per Sprint 002.5 §5 IA — "Permissions"
 * is a screen reached from Settings, not a top-level destination),
 * Notifications, and Real-Time — every destination from Sprint 002.5's
 * screen inventory that isn't itself a sub-state of another screen.
 *
 * Every route below renders only a placeholder (Sprint 003 Task 4:
 * "No feature logic"). Deep-link handling for notification tap-through
 * (Sprint 002.75 §12) is a Sprint 004+ addition once real notifications
 * exist.
 */
@Composable
fun SpaceAntivirusNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SpaceAntivirusBottomBar(navController = navController, currentRoute = currentRoute)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = OnboardingNavigationRoute,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding),
        ) {
            composable(OnboardingNavigationRoute) { OnboardingRoute() }
            composable(HomeNavigationRoute) { HomeRoute() }
            composable(SecurityCenterNavigationRoute) { SecurityCenterRoute() }
            composable(CleanNavigationRoute) { CleanRoute() }
            composable(SettingsNavigationRoute) { SettingsRoute() }
            composable(PremiumNavigationRoute) { PremiumRoute() }
            composable(HistoryNavigationRoute) { HistoryRoute() }
            composable(NotificationsNavigationRoute) { NotificationsRoute() }
            composable(RealTimeNavigationRoute) { RealTimeRoute() }
        }
    }
}

@Composable
private fun SpaceAntivirusBottomBar(
    navController: NavHostController,
    currentRoute: String?,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}
