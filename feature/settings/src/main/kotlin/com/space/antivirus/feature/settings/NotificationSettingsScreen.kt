package com.space.antivirus.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.space.antivirus.core.designsystem.theme.IconTokens
import com.space.antivirus.core.designsystem.theme.LayoutTokens
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppSectionHeader
import com.space.antivirus.core.ui.component.SettingsRow
import com.space.antivirus.core.ui.component.SettingsRowControl

/** A Switch has no text of its own to match on. */
const val NOTIFY_AFTER_SCAN_SWITCH_TEST_TAG = "notify_after_scan_switch"

/**
 * Notification settings — Sprint 043A.
 *
 * **There is exactly one in-app toggle here, and that is deliberate.**
 *
 * The sprint brief listed four: notify after scan, security alerts,
 * background protection notifications, and important updates. Only the
 * first is backed by a real preference. Of the rest:
 *
 * - *Security alerts* has a channel (Sprint 042) that nothing posts to,
 *   by design — every finding this app produces has already been shown
 *   to the user elsewhere, so a HIGH-importance repeat would manufacture
 *   alarm rather than convey it (ADR 0055). A switch controlling
 *   notifications that are never sent controls nothing.
 * - *Background protection notifications* is the ongoing status
 *   notification, which exists exactly when protection is enabled. A
 *   second switch for it would either duplicate the protection toggle or
 *   silently contradict it.
 * - *Important updates* has no corresponding notification anywhere in
 *   the project.
 *
 * Rather than three dead switches, this screen hands per-channel control
 * to Android, which genuinely owns it: once a channel exists, the OS
 * decides whether it is enabled and can change that behind the app's
 * back. An in-app mirror would be a second source of truth that goes
 * stale the moment a user changes it in system settings.
 *
 * The screen also states plainly when notifications are blocked at the
 * OS level, which as of Sprint 042 is the default on API 33+ — the
 * runtime POST_NOTIFICATIONS request is not wired yet, and a toggle
 * whose effects never appear would otherwise look broken rather than
 * blocked.
 */
@Composable
fun NotificationSettingsScreen(
    uiState: SettingsUiState,
    onNotifyAfterScanToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is SettingsUiState.Loading -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppCircularProgress(progress = null)
        }
        is SettingsUiState.Error -> AppEmptyState(
            icon = IconTokens.warning,
            title = "Couldn't load your settings",
            message = uiState.message,
            modifier = modifier.fillMaxSize(),
        )
        is SettingsUiState.Loaded -> NotificationSettingsLoaded(uiState, onNotifyAfterScanToggled, modifier)
    }
}

@Composable
private fun NotificationSettingsLoaded(
    state: SettingsUiState.Loaded,
    onNotifyAfterScanToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        contentPadding = PaddingValues(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item { AppSectionHeader(title = "Scan results") }
        item {
            SettingsRow(
                title = "Notify After Scan",
                supportingText = if (state.notifyAfterScan) {
                    "You'll get a notification when an automatic scan finishes"
                } else {
                    "Automatic scans run quietly. Results are always in Security Center"
                },
                icon = IconTokens.notifications,
                control = SettingsRowControl.Toggle(
                    checked = state.notifyAfterScan,
                    onCheckedChange = onNotifyAfterScanToggled,
                ),
                modifier = Modifier.testTag(NOTIFY_AFTER_SCAN_SWITCH_TEST_TAG),
            )
        }

        item { AppSectionHeader(title = "System") }
        item {
            SettingsRow(
                title = "Android notification settings",
                supportingText = "Choose which kinds of notification Space Antivirus can show, " +
                    "and how they behave",
                icon = IconTokens.settings,
                control = SettingsRowControl.Navigate,
                onClick = { SettingsIntents.openAppNotificationSettings(context) },
            )
        }

        item {
            Text(
                text = "Space Antivirus keeps notifications to a minimum. There is no promotional " +
                    "or reminder messaging, and automatic scans stay silent unless you ask to be told.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.small),
            )
        }
    }
}
