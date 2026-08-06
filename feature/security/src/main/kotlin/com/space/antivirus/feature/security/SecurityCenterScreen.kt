package com.space.antivirus.feature.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.theme.IconTokens
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppTextButton
import com.space.antivirus.core.ui.component.EmptyStateTone
import com.space.antivirus.core.ui.component.EvidenceIcon
import com.space.antivirus.core.ui.component.ScanSummaryCard
import com.space.antivirus.core.ui.component.Severity
import com.space.antivirus.core.ui.component.ThreatSummaryCard
import java.text.DateFormat
import java.util.Date

/**
 * Follows ADR 0030's stateful/stateless split exactly, same as Home
 * (Sprint 017) and Onboarding (Sprint 018). Deliberately uses only
 * Icons.Default.Warning — the one icon Sprint 017's verification
 * confirmed is genuinely part of this project's baseline (non-Extended)
 * Material icon set. No other icon is introduced directly in THIS file
 * — ThreatSummaryCard (core:ui) owns its own, separately-reasoned icon
 * choices.
 *
 * Sprint 021: gained onViewHistoryClick — History (feature:history) was
 * previously unreachable anywhere in the real app (not one of the 4
 * bottom-nav destinations, nothing else linked to it). This screen is
 * the natural place for that entry point, reusing the exact same
 * callback-based navigation pattern already established for onboarding
 * completion (Sprint 018) rather than inventing something new.
 *
 * Sprint 030 (ADR 0044): now renders ThreatSummaryCard (core:ui) instead
 * of a bespoke card built inline here — the shared component both this
 * screen and History use. Two real Android Intents are launched
 * directly from this screen via LocalContext.current, deliberately not
 * routed through the ViewModel: a ViewModel should never hold a Context
 * reference. Both are standard, permission-free Intents any app may
 * send — ACTION_APPLICATION_DETAILS_SETTINGS opens the system's own App
 * Info screen; ACTION_DELETE with a package: Uri opens the system's own
 * uninstall confirmation dialog. Neither performs the action directly;
 * both hand off to system UI the user must explicitly confirm, so
 * neither needs any new permission this app doesn't already have.
 */
@Composable
fun SecurityCenterRoute(
    onViewHistoryClick: () -> Unit,
    viewModel: SecurityCenterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SecurityCenterScreen(
        uiState = uiState,
        onViewHistoryClick = onViewHistoryClick,
        onIgnoreClick = viewModel::onIgnoreClick,
    )
}

@Composable
fun SecurityCenterScreen(
    uiState: SecurityCenterUiState,
    onViewHistoryClick: () -> Unit,
    onIgnoreClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is SecurityCenterUiState.Loading -> SecurityCenterLoading(modifier)
        is SecurityCenterUiState.Loaded ->
            SecurityCenterLoaded(uiState, onViewHistoryClick, onIgnoreClick, modifier)
        is SecurityCenterUiState.Error -> SecurityCenterError(uiState, modifier)
    }
}

@Composable
private fun SecurityCenterLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppCircularProgress(progress = null)
    }
}

@Composable
private fun SecurityCenterError(state: SecurityCenterUiState.Error, modifier: Modifier = Modifier) {
    AppEmptyState(icon = Icons.Default.Warning, message = state.message, modifier = modifier)
}

@Composable
private fun SecurityCenterLoaded(
    state: SecurityCenterUiState.Loaded,
    onViewHistoryClick: () -> Unit,
    onIgnoreClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            when {
                state.protectionStatus == ProtectionStatus.UNKNOWN -> AppEmptyState(
                    icon = IconTokens.scan,
                    title = "No scan results yet",
                    message = "Run a scan from Home and your security status will appear here.",
                    modifier = Modifier.fillMaxSize(),
                )
                // Sprint 041: this was rendering a warning triangle for
                // a genuinely clean result. A finished scan that found
                // nothing is good news, and dressing it as a problem is
                // the same exaggeration ADR 0015 rules out for findings.
                state.threats.isEmpty() -> AppEmptyState(
                    icon = IconTokens.trusted,
                    title = "Nothing to review",
                    message = "Your last scan completed and didn't flag anything.",
                    tone = EmptyStateTone.POSITIVE,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                ) {
                    state.scanSummary?.let { summary ->
                        item {
                            ScanSummaryCard(
                                isProtected = state.protectionStatus == ProtectionStatus.PROTECTED,
                                lastScanText = state.lastScanCompletedAtEpochMillis
                                    ?.let { DateFormat.getDateTimeInstance().format(Date(it)) }
                                    ?: "Unknown",
                                appsScanned = summary.appsScanned,
                                findingsCount = summary.threatsDetected,
                                trustedCount = summary.trustedApps,
                                infoCount = state.threats.count { it.riskLevel == RiskLevel.INFO },
                                attentionCount = state.threats.count { it.riskLevel == RiskLevel.ATTENTION },
                                highRiskCount = state.threats.count { it.riskLevel == RiskLevel.ACTION_NEEDED },
                                ignoredCount = summary.ignoredThreats,
                                scanDurationLabel = "${"%.1f".format(summary.scanDurationMillis / 1000.0)}s",
                                // Sprint 041: a real Severity so the card
                                // can render the app's own badge instead of
                                // a bare string. Same purely UI-layer
                                // aggregation HistoryScreen already does;
                                // ScanSummary.highestThreatLabel is
                                // untouched and still computed as before.
                                highestSeverity = state.threats
                                    .maxByOrNull { it.riskLevel.ordinal }
                                    ?.riskLevel
                                    ?.toSeverity(),
                                averageConfidenceLabel = summary.averageConfidenceLabel,
                            )
                        }
                    }
                    items(state.threats) { threat ->
                        ThreatCard(
                            threat = threat,
                            onIgnoreClick = { onIgnoreClick(threat.packageName) },
                            onOpenAppInfoClick = { openAppInfo(context, threat.packageName) },
                            onUninstallClick = { requestUninstall(context, threat.packageName) },
                        )
                    }
                }
            }
        }
        AppTextButton(
            text = "View full history",
            onClick = onViewHistoryClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
        )
    }
}

@Composable
private fun ThreatCard(
    threat: ThreatSummary,
    onIgnoreClick: () -> Unit,
    onOpenAppInfoClick: () -> Unit,
    onUninstallClick: () -> Unit,
) {
    ThreatSummaryCard(
        appLabel = threat.appLabel,
        packageName = threat.packageName,
        severity = threat.riskLevel.toSeverity(),
        evidenceIcons = threat.evidenceBullets.flatMap { EvidenceIcon.inferFrom(it) }.toSet(),
        threatCategory = threat.threatCategory,
        shortSummary = threat.shortSummary,
        technicalDetail = threat.technicalDetail,
        evidenceBullets = threat.evidenceBullets,
        recommendation = threat.recommendation,
        confidenceLabel = threat.confidenceLabel,
        onIgnoreClick = onIgnoreClick,
        onOpenAppInfoClick = onOpenAppInfoClick,
        onUninstallClick = onUninstallClick,
    )
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

/**
 * Sprint 047 removed the Sprint 32.1/32.3/32.4 diagnostic instrumentation
 * that surrounded this function, which its own comments marked "temporary,
 * remove before release".
 *
 * **The fix it was investigating is kept in full** —
 * `FLAG_ACTIVITY_NEW_TASK`, below, along with the reasoning for it. What
 * is gone is the logging and the read-only `PackageManager` probes that
 * existed only to feed it: `getApplicationInfo`, the deprecated
 * `getInstallerPackageName`, and `resolveActivity`. Those ran on every
 * uninstall tap in production, on the main thread, purely to produce
 * Logcat output nobody was reading, and one of them was a deprecated API
 * held in place by a `@Suppress`.
 *
 * The `startActivity` call is left unguarded, as it was before the
 * diagnostics: it is a normal call that either works or throws, and
 * swallowing the exception was a debugging affordance, not error
 * handling.
 */
private fun requestUninstall(context: Context, packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE).apply {
        data = Uri.fromParts("package", packageName, null)
        // Sprint 32.1. The reported symptom — startActivity() returning
        // normally with no exception, but the system uninstall
        // confirmation never appearing — is a documented pattern when a
        // foreground-launched Intent targets a separate task (here the
        // system package installer, a different app entirely) without
        // this flag. Android can silently decline to bring the new
        // activity forward rather than throwing.
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
