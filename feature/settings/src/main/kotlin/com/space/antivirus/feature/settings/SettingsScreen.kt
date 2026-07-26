package com.space.antivirus.feature.settings

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
    SettingsScreen(
        uiState = uiState,
        onBackgroundProtectionToggled = viewModel::onBackgroundProtectionToggled,
        onIntervalSelected = viewModel::onIntervalSelected,
        onDismissError = viewModel::dismissError,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBackgroundProtectionToggled: (Boolean) -> Unit,
    onIntervalSelected: (ScanInterval) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is SettingsUiState.Loading -> SettingsLoading(modifier)
        is SettingsUiState.Loaded -> SettingsLoaded(
            state = uiState,
            onBackgroundProtectionToggled = onBackgroundProtectionToggled,
            onIntervalSelected = onIntervalSelected,
            onDismissError = onDismissError,
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
    onIntervalSelected: (ScanInterval) -> Unit,
    onDismissError: () -> Unit,
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
        }
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
