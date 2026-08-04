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
import androidx.compose.ui.text.style.TextAlign
import com.space.antivirus.core.designsystem.theme.IconTokens
import com.space.antivirus.core.designsystem.theme.LayoutTokens
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppSectionHeader
import com.space.antivirus.core.ui.component.SettingsRow
import com.space.antivirus.core.ui.component.SettingsRowControl

/**
 * Scan interval, on its own screen — Sprint 043A.
 *
 * Sprint 026 put this in the hub as a row of text buttons, which was
 * fine when the hub had three rows. As a single-choice list it belongs
 * on its own screen with real radio semantics.
 *
 * No new preference and no new business logic: the same
 * `SetScanIntervalUseCase` and the same `ProtectionManager` re-schedule
 * path as before, called from the same `SettingsViewModel` method. This
 * screen only changes where the choice is presented.
 *
 * The whole list disables when background protection is off. That is the
 * honest state: nothing is scheduled, so an interval is a preference
 * with nothing to apply it to — it still persists, and takes effect the
 * moment protection is switched on.
 */
@Composable
fun ScheduledScanScreen(
    uiState: SettingsUiState,
    onIntervalSelected: (ScanInterval) -> Unit,
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
        is SettingsUiState.Loaded -> ScheduledScanLoaded(uiState, onIntervalSelected, modifier)
    }
}

@Composable
private fun ScheduledScanLoaded(
    state: SettingsUiState.Loaded,
    onIntervalSelected: (ScanInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        contentPadding = PaddingValues(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item { AppSectionHeader(title = "How often should we scan?") }

        items(ScanInterval.entries.size) { index ->
            val interval = ScanInterval.entries[index]
            SettingsRow(
                title = interval.label,
                supportingText = interval.description(),
                icon = IconTokens.schedule,
                control = SettingsRowControl.Selection(selected = state.selectedInterval == interval),
                enabled = state.backgroundProtectionEnabled,
                onClick = { onIntervalSelected(interval) },
            )
        }

        item {
            Text(
                text = if (state.backgroundProtectionEnabled) {
                    "Scans run in the background and are timed around your battery and storage, " +
                        "so the exact moment can shift."
                } else {
                    "Background protection is off, so nothing is scheduled yet. Your choice is " +
                        "saved and takes effect as soon as you turn it on."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = spacing.small),
            )
        }
    }
}

/** Plain-language framing of each interval's trade-off. */
private fun ScanInterval.description(): String = when (this) {
    ScanInterval.DAILY -> "Most thorough. Uses slightly more battery."
    ScanInterval.EVERY_3_DAYS -> "A balance of coverage and battery."
    ScanInterval.WEEKLY -> "Lightest on battery."
}
