package com.space.antivirus.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.ui.component.AppCard
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppTextButton
import java.text.DateFormat
import java.util.Date

/** Exposed so SettingsScreenTest can reliably find the Switch — it has
 *  no text of its own to match on. */
const val BACKGROUND_PROTECTION_SWITCH_TEST_TAG = "background_protection_switch"

/** Sprint 042. Same reason: a Switch has no text of its own. */
const val NOTIFY_AFTER_SCAN_SWITCH_TEST_TAG = "notify_after_scan_switch"

/**
 * Replaces the Sprint 003 placeholder. Follows ADR 0030's stateful/
 * stateless split exactly, same as every prior feature screen. Only
 * Icons.Default.Warning used — the one icon confirmed genuinely
 * baseline-safe since Sprint 017's verification. Switch is a standard
 * Material3 component, already used throughout this project's design
 * system dependency, no new icon or component risk introduced.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        uiState = uiState,
        onBackgroundProtectionToggled = viewModel::onBackgroundProtectionToggled,
        onNotifyAfterScanToggled = viewModel::onNotifyAfterScanToggled,
        onIntervalSelected = viewModel::onIntervalSelected,
        onDismissError = viewModel::dismissError,
        // Sprint 042: launched from the screen, not the ViewModel — a
        // ViewModel never holds a Context, the same rule Security Center
        // and History already follow for their own system Intents.
        // ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS opens the system's
        // own list; it asks for nothing and changes nothing on its own.
        onOpenBatterySettings = { openBatteryOptimizationSettings(context) },
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBackgroundProtectionToggled: (Boolean) -> Unit,
    onIntervalSelected: (ScanInterval) -> Unit,
    onDismissError: () -> Unit,
    onNotifyAfterScanToggled: (Boolean) -> Unit = {},
    onOpenBatterySettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is SettingsUiState.Loading -> SettingsLoading(modifier)
        is SettingsUiState.Loaded -> SettingsLoaded(
            state = uiState,
            onBackgroundProtectionToggled = onBackgroundProtectionToggled,
            onNotifyAfterScanToggled = onNotifyAfterScanToggled,
            onIntervalSelected = onIntervalSelected,
            onDismissError = onDismissError,
            onOpenBatterySettings = onOpenBatterySettings,
            modifier = modifier,
        )
        is SettingsUiState.Error -> SettingsError(uiState, modifier)
    }
}

@Composable
private fun SettingsLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppCircularProgress(progress = null)
    }
}

@Composable
private fun SettingsError(state: SettingsUiState.Error, modifier: Modifier = Modifier) {
    AppEmptyState(icon = Icons.Default.Warning, message = state.message, modifier = modifier)
}

@Composable
private fun SettingsLoaded(
    state: SettingsUiState.Loaded,
    onBackgroundProtectionToggled: (Boolean) -> Unit,
    onNotifyAfterScanToggled: (Boolean) -> Unit,
    onIntervalSelected: (ScanInterval) -> Unit,
    onDismissError: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        if (state.errorMessage != null) {
            AppCard(headline = "Something went wrong", supportingText = state.errorMessage) {
                AppTextButton(text = "Dismiss", onClick = onDismissError)
            }
        }

        BackgroundProtectionCard(
            enabled = state.backgroundProtectionEnabled,
            lastScheduledAtEpochMillis = state.lastScheduledAtEpochMillis,
            onToggled = onBackgroundProtectionToggled,
        )

        if (state.backgroundProtectionEnabled) {
            ScanIntervalCard(selectedInterval = state.selectedInterval, onIntervalSelected = onIntervalSelected)

            NotifyAfterScanCard(enabled = state.notifyAfterScan, onToggled = onNotifyAfterScanToggled)

            // Sprint 042. Shown only when protection is on AND the
            // standard allowlist actually reports this app as
            // restricted — an informational card about a problem the
            // user doesn't have is just noise.
            if (!state.isIgnoringBatteryOptimizations) {
                BatteryOptimizationCard(onOpenSettings = onOpenBatterySettings)
            }
        }
    }
}

/**
 * Sprint 042. Off by default, and the supporting copy says why rather
 * than just what: a security app that pings after every routine scan
 * that found nothing trains you to dismiss it, and a notification you've
 * learned to ignore is worse than none when something is actually wrong.
 */
@Composable
private fun NotifyAfterScanCard(enabled: Boolean, onToggled: (Boolean) -> Unit) {
    AppCard(
        headline = "Notify After Scan",
        supportingText = if (enabled) {
            "On \u2014 you'll get a notification when an automatic scan finishes"
        } else {
            "Off \u2014 automatic scans run quietly. You'll still see results in Security Center"
        },
    ) {
        Switch(
            checked = enabled,
            onCheckedChange = onToggled,
            modifier = Modifier.testTag(NOTIFY_AFTER_SCAN_SWITCH_TEST_TAG),
        )
    }
}

/**
 * Sprint 042. Informational, one action, no nagging.
 *
 * This app never requests the battery-optimisation exemption directly:
 * that Intent is Play-restricted to a narrow set of app categories, and
 * a security app pushing its way onto an unrestricted battery allowlist
 * is exactly the behaviour that earns this category its reputation. The
 * card explains the trade-off and opens the system's own settings
 * screen, where the user decides. The copy is careful not to overstate
 * the benefit — Android may still defer scheduled work, and several
 * manufacturers layer their own process management on top of the
 * standard allowlist that this check cannot see.
 */
@Composable
private fun BatteryOptimizationCard(onOpenSettings: () -> Unit) {
    AppCard(
        headline = "Battery optimisation is on",
        supportingText = "Android may delay scheduled scans to save battery. Allowing unrestricted " +
            "battery usage makes them more likely to run on time. This is optional \u2014 protection " +
            "still works either way.",
    ) {
        AppTextButton(text = "Open battery settings", onClick = onOpenSettings)
    }
}

@Composable
private fun BackgroundProtectionCard(
    enabled: Boolean,
    lastScheduledAtEpochMillis: Long?,
    onToggled: (Boolean) -> Unit,
) {
    val supportingText = if (enabled) {
        val formattedDate = lastScheduledAtEpochMillis?.let {
            DateFormat.getDateTimeInstance().format(Date(it))
        }
        if (formattedDate != null) {
            "On \u2014 last scheduled $formattedDate"
        } else {
            "On"
        }
    } else {
        "Off"
    }

    AppCard(headline = "Background Protection", supportingText = supportingText) {
        Switch(
            checked = enabled,
            onCheckedChange = onToggled,
            modifier = Modifier.testTag(BACKGROUND_PROTECTION_SWITCH_TEST_TAG),
        )
    }
}

@Composable
private fun ScanIntervalCard(selectedInterval: ScanInterval, onIntervalSelected: (ScanInterval) -> Unit) {
    val spacing = LocalSpacing.current
    AppCard(headline = "Scan Interval", supportingText = "How often to check for threats automatically") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            ScanInterval.entries.forEach { interval ->
                IntervalOptionRow(
                    interval = interval,
                    isSelected = interval == selectedInterval,
                    onSelected = { onIntervalSelected(interval) },
                )
            }
        }
    }
}

@Composable
private fun IntervalOptionRow(interval: ScanInterval, isSelected: Boolean, onSelected: () -> Unit) {
    val label = if (isSelected) "${interval.label} (selected)" else interval.label
    AppTextButton(text = label, onClick = onSelected, enabled = !isSelected)
}

/**
 * Sprint 042. Opens the system's own battery-optimisation list.
 *
 * Deliberately ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, never
 * ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: the latter puts up a
 * direct "allow this app to always run" prompt and is Play-restricted to
 * a narrow set of app categories. This one just opens settings, asks for
 * nothing, and needs no permission — the same standard, permission-free
 * hand-off to system UI that Security Center's App Info and uninstall
 * Intents already use.
 */
private fun openBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Not every OEM build exposes this screen. Failing silently is
        // correct here: the card is informational, nothing depends on
        // the user reaching it, and protection works either way.
    }
}
