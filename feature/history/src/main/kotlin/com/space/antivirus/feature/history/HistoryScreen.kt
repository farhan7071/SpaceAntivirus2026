package com.space.antivirus.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.ui.component.AppCard
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.Severity
import com.space.antivirus.core.ui.component.StatusChip
import java.text.DateFormat
import java.util.Date

/**
 * Replaces the Sprint 003 placeholder. Follows ADR 0030's stateful/
 * stateless split exactly, same as every prior feature screen. Only
 * Icons.Default.Warning used — the one icon confirmed genuinely baseline-
 * safe since Sprint 017's verification.
 *
 * Reached from Security Center's new "View full history" entry point
 * (Sprint 021) — History was previously unreachable anywhere in the real
 * app: it's deliberately not one of the 4 bottom-nav destinations
 * (TopLevelDestination's own KDoc — Home/Security Center/Clean/Settings
 * only), and nothing else linked to it. Building this screen without
 * also wiring a way to reach it would have left it inaccessible, so this
 * sprint adds that one entry point too, reusing the exact same callback-
 * based navigation pattern already established for onboarding
 * completion (Sprint 018) rather than inventing something new.
 */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(uiState = uiState)
}

@Composable
fun HistoryScreen(uiState: HistoryUiState, modifier: Modifier = Modifier) {
    when (uiState) {
        is HistoryUiState.Loading -> HistoryLoading(modifier)
        is HistoryUiState.Loaded -> HistoryLoaded(uiState, modifier)
        is HistoryUiState.Error -> HistoryError(uiState, modifier)
    }
}

@Composable
private fun HistoryLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppCircularProgress(progress = null)
    }
}

@Composable
private fun HistoryError(state: HistoryUiState.Error, modifier: Modifier = Modifier) {
    AppEmptyState(icon = Icons.Default.Warning, message = state.message, modifier = modifier)
}

@Composable
private fun HistoryLoaded(state: HistoryUiState.Loaded, modifier: Modifier = Modifier) {
    if (state.entries.isEmpty()) {
        AppEmptyState(
            icon = Icons.Default.Warning,
            message = "No scans yet. Run a scan from Home to see your history here.",
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        items(state.entries) { entry -> ScanHistoryEntryCard(entry) }
    }
}

@Composable
private fun ScanHistoryEntryCard(entry: ScanHistoryEntry) {
    val spacing = LocalSpacing.current
    val formattedDate = DateFormat.getDateTimeInstance().format(Date(entry.completedAtEpochMillis))
    val durationSeconds = entry.durationMillis / 1000.0
    val resultText = if (entry.isClean) {
        "No threats found"
    } else {
        "${entry.threats.size} item(s) found"
    }
    val supportingText = "${entry.itemsScanned} apps scanned in ${"%.1f".format(durationSeconds)}s \u00B7 $resultText"

    AppCard(headline = formattedDate, supportingText = supportingText) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (entry.isClean) {
                StatusChip(Severity.INFO)
            } else {
                StatusChip(Severity.ATTENTION)
                entry.threats.forEach { threat -> ThreatSummaryRow(threat) }
            }
        }
    }
}

@Composable
private fun ThreatSummaryRow(threat: ThreatSummary) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(text = threat.title, style = MaterialTheme.typography.titleSmall)
        Text(text = threat.description, style = MaterialTheme.typography.bodyMedium)
        StatusChip(threat.riskLevel.toSeverity())
    }
}

private fun RiskLevel.toSeverity(): Severity = when (this) {
    RiskLevel.INFO -> Severity.INFO
    RiskLevel.ATTENTION -> Severity.ATTENTION
    RiskLevel.ACTION_NEEDED -> Severity.ACTION_NEEDED
}
