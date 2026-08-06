package com.space.antivirus.viruscleaner.mobilesecurity.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.space.antivirus.feature.settings.AboutNavigationRoute
import com.space.antivirus.feature.settings.AboutRoute
import com.space.antivirus.feature.settings.IgnoreListNavigationRoute
import com.space.antivirus.feature.settings.IgnoreListRoute
import com.space.antivirus.feature.settings.NotificationSettingsNavigationRoute
import com.space.antivirus.feature.settings.NotificationSettingsRoute
import com.space.antivirus.feature.settings.ScheduledScanNavigationRoute
import com.space.antivirus.feature.settings.ScheduledScanRoute
import com.space.antivirus.feature.settings.SettingsNavigationRoute
import com.space.antivirus.feature.settings.SettingsRoute

/**
 * The full navigation skeleton required by Sprint 003 Task 4: Home,
 * Security Center, Clean, Settings, Onboarding, Premium, History,
 * Permissions (folded into Settings per Sprint 002.5 section 5 IA — "Permissions"
 * is a screen reached from Settings, not a top-level destination),
 * Notifications, and Real-Time — every destination from Sprint 002.5's
 * screen inventory that isn't itself a sub-state of another screen.
 *
 * Every route below rendered only a placeholder as of Sprint 003 Task 4
 * ("No feature logic"). Real screens landed in: Onboarding (Sprint 018),
 * Home (Sprint 017), Security Center (Sprint 019), History (Sprint 021,
 * reached only via Security Center's "View full history" — deliberately
 * not a 5th bottom-nav tab, see TopLevelDestination's own KDoc). Clean,
 * Settings, Premium, Notifications, and Real-Time remain placeholders
 * pending their own Phase C+ sprints. Deep-link handling for
 * notification tap-through (Sprint 002.75 section 12) is a later addition once
 * real notifications exist.
 */
@Composable
fun SpaceAntivirusNavHost(
    navController: NavHostController = rememberNavController(),
    /**
     * Sprint 044. Invoked once the user has read and dismissed a scan
     * result — the app's only interstitial moment. Defaulted to a no-op
     * so previews and tests need no ads wiring.
     */
    onScanResultAcknowledged: () -> Unit = {},
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
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(OnboardingNavigationRoute) {
                OnboardingRoute(
                    onOnboardingComplete = {
                        navController.navigate(HomeNavigationRoute) {
                            popUpTo(OnboardingNavigationRoute) { inclusive = true }
                        }
                    },
                )
            }
            composable(HomeNavigationRoute) {
                HomeRoute(
                    onScanResultAcknowledged = onScanResultAcknowledged,
                    onNavigateToSecurityCenter = { navController.navigate(SecurityCenterNavigationRoute) },
                    onNavigateToCleaner = { navController.navigate(CleanNavigationRoute) },
                    onNavigateToHistory = { navController.navigate(HistoryNavigationRoute) },
                    onNavigateToSettings = { navController.navigate(SettingsNavigationRoute) },
                )
            }
            composable(SecurityCenterNavigationRoute) {
                SecurityCenterRoute(
                    onViewHistoryClick = { navController.navigate(HistoryNavigationRoute) },
                )
            }
            composable(CleanNavigationRoute) { CleanRoute() }
            composable(SettingsNavigationRoute) {
                SettingsRoute(
                    onNavigateToScheduledScan = { navController.navigate(ScheduledScanNavigationRoute) },
                    onNavigateToNotifications = {
                        navController.navigate(NotificationSettingsNavigationRoute)
                    },
                    onNavigateToIgnoreList = { navController.navigate(IgnoreListNavigationRoute) },
                    onNavigateToAbout = { navController.navigate(AboutNavigationRoute) },
                )
            }
            // Sprint 043A leaf destinations. Plain string routes, per
            // ADR 0009's existing convention — a UI sprint is not the
            // place to migrate half the graph to type-safe routes.
            composable(ScheduledScanNavigationRoute) { ScheduledScanRoute() }
            composable(NotificationSettingsNavigationRoute) { NotificationSettingsRoute() }
            composable(IgnoreListNavigationRoute) { IgnoreListRoute() }
            composable(AboutNavigationRoute) { AboutRoute() }
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
                label = {
                    // Sprint 046.2. "Security Center" is the only label
                    // that wraps to two lines, which made its whole item
                    // taller than the other three — NavigationBarItem
                    // stacks icon above label, so that item's icon rode
                    // visibly higher than its neighbours and the pair
                    // stopped reading as one centred unit.
                    //
                    // Fixed by giving every label the same two-line box
                    // and centring its text inside it, rather than by
                    // shortening the word. All four items now have
                    // identical content height, so all four icons sit on
                    // one line, and each label is centred under its own
                    // icon whether it takes one line or two.
                    Box(
                        modifier = Modifier.height(NAV_LABEL_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = destination.label,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                },
            )
        }
    }
}

/**
 * Two lines of the navigation bar's own label style. Fixed rather than
 * measured: the point is that every item reserves the same height
 * regardless of how many lines its own label needs.
 */
private val NAV_LABEL_HEIGHT = 32.dp
