package com.space.antivirus.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Navigation route constants for the Settings section. Kept as plain
 * consts rather than type-safe Navigation-Compose objects, per ADR 0009
 * — Sprint 043A follows the existing convention rather than migrating
 * half the graph in a UI sprint.
 */
const val SettingsNavigationRoute = "settings"

/** Sprint 043A leaf destinations. */
const val ScheduledScanNavigationRoute = "settings/scheduled_scan"
const val NotificationSettingsNavigationRoute = "settings/notifications"
const val IgnoreListNavigationRoute = "settings/ignore_list"
const val AboutNavigationRoute = "settings/about"

@Composable
fun SettingsRoute(
    onNavigateToScheduledScan: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToIgnoreList: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onBackgroundProtectionToggled = viewModel::onBackgroundProtectionToggled,
        onDismissError = viewModel::dismissError,
        onNavigateToScheduledScan = onNavigateToScheduledScan,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToIgnoreList = onNavigateToIgnoreList,
        onNavigateToAbout = onNavigateToAbout,
        modifier = modifier,
    )
}

@Composable
fun ScheduledScanRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScheduledScanScreen(
        uiState = uiState,
        onIntervalSelected = viewModel::onIntervalSelected,
        modifier = modifier,
    )
}

@Composable
fun NotificationSettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationSettingsScreen(
        uiState = uiState,
        onNotifyAfterScanToggled = viewModel::onNotifyAfterScanToggled,
        modifier = modifier,
    )
}

@Composable
fun AboutRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AboutScreen(uiState = uiState, modifier = modifier)
}
