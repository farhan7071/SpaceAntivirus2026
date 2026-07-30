package com.space.antivirus.feature.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.ui.component.AppCard
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.EvidenceIcon
import com.space.antivirus.core.ui.component.ScanResultBadge
import com.space.antivirus.core.ui.component.Severity
import com.space.antivirus.core.ui.component.ThreatSummaryCard
import java.text.DateFormat
import java.util.Date

/**
 * Replaces the Sprint 003 placeholder. Follows ADR 0030's stateful/
 * stateless split exactly, same as every prior feature screen. Only
 * Icons.Default.Warning used directly in THIS file — the one icon
 * confirmed genuinely baseline-safe since Sprint 017's verification;
 * ThreatSummaryCard (core:ui) owns its own, separately-reasoned icon
 * choices.
 *
 * Reached from Security Center's "View full history" entry point
 * (Sprint 021) — History was previously unreachable anywhere in the real
 * app: it's deliberately not one of the 4 bottom-nav destinations
 * (TopLevelDestination's own KDoc — Home/Security Center/Clean/Settings
 * only), and nothing else linked to it.
 *
 * Sprint 030 (ADR 0044): each scan session's own threats now render as
 * ThreatSummaryCard (core:ui) — the same shared component
 * SecurityCenterScreen uses, directly satisfying "both screens should
 * share the same UI components where practical." The outer per-session
 * AppCard (date, apps-scanned/duration summary) is kept as-is — that's a
 * genuinely different kind of card (one scan session, not one flagged
 * app) that ThreatSummaryCard was never meant to replace; only the
 * per-app content INSIDE each session's card changed. Open App Info and
 * Uninstall are wired identically to SecurityCenterScreen — real,
 * standard, permission-free Android Intents launched directly from this
 * screen via LocalContext.current, never through the ViewModel.
 */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(uiState = uiState, onIgnoreClick = viewModel::onIgnoreClick)
}

@Composable
fun HistoryScreen(uiState: HistoryUiState, onIgnoreClick: (String) -> Unit, modifier: Modifier = Modifier) {
    when (uiState) {
        is HistoryUiState.Loading -> HistoryLoading(modifier)
        is HistoryUiState.Loaded -> HistoryLoaded(uiState, onIgnoreClick, modifier)
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
private fun HistoryLoaded(
    state: HistoryUiState.Loaded,
    onIgnoreClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        items(state.entries) { entry -> ScanHistoryEntryCard(entry, onIgnoreClick) }
    }
}

/**
 * Sprint 034 (Part 7 — "Each scan session should become its own card...
 * Display: Scan date, Scan time, Apps scanned, Security findings,
 * Highest severity, Duration. Use a colored badge indicating overall
 * scan result"): redesigned from one cramped supportingText line
 * ("467 apps scanned in 2.3s · 42 item(s) found") into the fields shown
 * separately, plus ScanResultBadge (core:ui, new this sprint) replacing
 * the previous StatusChip(Severity.INFO) misuse for clean sessions —
 * see that component's own KDoc for why that was a real, pre-existing
 * bug, not a style choice. Highest severity is the highest Severity
 * among this session's own threats, the same purely UI-layer
 * aggregation ScanSummaryCard's severity breakdown already performs in
 * SecurityCenterScreen.kt — no new business logic, ScanHistoryEntry
 * itself unchanged.
 */
@Composable
private fun ScanHistoryEntryCard(entry: ScanHistoryEntry, onIgnoreClick: (String) -> Unit) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val formattedDate = DateFormat.getDateTimeInstance().format(Date(entry.completedAtEpochMillis))
    val durationSeconds = entry.durationMillis / 1000.0
    val highestSeverity = entry.threats.maxByOrNull { it.riskLevel.ordinal }?.riskLevel?.toSeverity()
        ?: Severity.INFO

    AppCard(headline = formattedDate) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScanResultBadge(isClean = entry.isClean, highestSeverity = highestSeverity)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${entry.itemsScanned} apps scanned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${entry.threats.size} findings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${"%.1f".format(durationSeconds)}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!entry.isClean) {
                entry.threats.forEach { threat ->
                    ThreatSummaryCard(
                        appLabel = threat.appLabel,
                        packageName = threat.packageName,
                        severity = threat.riskLevel.toSeverity(),
                        threat.threatCategory,
                        evidenceIcons = threat.evidenceBullets.flatMap { EvidenceIcon.inferFrom(it) }.toSet(),
                        shortSummary = threat.shortSummary,
                        technicalDetail = threat.technicalDetail,
                        evidenceBullets = threat.evidenceBullets,
                        recommendation = threat.recommendation,
                        confidenceLabel = threat.confidenceLabel,
                        onIgnoreClick = { onIgnoreClick(threat.packageName) },
                        onOpenAppInfoClick = { openAppInfo(context, threat.packageName) },
                        onUninstallClick = { requestUninstall(context, threat.packageName) },
                    )
                }
            }
        }
    }
}

private fun RiskLevel.toSeverity(): Severity = when (this) {
    RiskLevel.INFO -> Severity.INFO
    RiskLevel.ATTENTION -> Severity.ATTENTION
    RiskLevel.ACTION_NEEDED -> Severity.ACTION_NEEDED
}

private fun openAppInfo(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    context.startActivity(intent)
}

private fun requestUninstall(context: Context, packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    context.startActivity(intent)
}
