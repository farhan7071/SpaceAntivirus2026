package com.space.antivirus.feature.security

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
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
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppTextButton
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
                    icon = Icons.Default.Warning,
                    message = "No scan results yet. Run a scan from Home to see your security status here.",
                    modifier = Modifier.fillMaxSize(),
                )
                state.threats.isEmpty() -> AppEmptyState(
                    icon = Icons.Default.Warning,
                    message = "No threats found. Your last scan didn't detect anything to review.",
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
                                highestSeverityLabel = summary.highestThreatLabel,
                                averageConfidenceLabel = summary.averageConfidenceLabel,
                            )
                        }
                    }
                    items(state.threats) { threat ->
                        ThreatCard(
                            threat = threat,
                            onIgnoreClick = {
                                // DIAGNOSTIC (Sprint 32.1) — temporary, remove before release
                                Log.d(
                                    "OverflowMenuDiag",
                                    "Ignore: onIgnoreClick callback, package=${threat.packageName}",
                                )
                                onIgnoreClick(threat.packageName)
                            },
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
    // DIAGNOSTIC (Sprint 32.1) — temporary, remove before release
    Log.d("OverflowMenuDiag", "OpenAppInfo: creating intent, package=$packageName")
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    // DIAGNOSTIC (Sprint 32.1) — temporary, remove before release
    Log.d("OverflowMenuDiag", "OpenAppInfo: calling startActivity()")
    context.startActivity(intent)
}

private fun requestUninstall(context: Context, packageName: String) {
    // DIAGNOSTIC (Sprint 32.1) — temporary, remove before release. Logs
    // the actual runtime type of context, not just that it's non-null —
    // if this ever prints something other than an Activity subclass, the
    // FLAG_ACTIVITY_NEW_TASK fix below may not be the whole story, and
    // that's worth knowing before assuming this fix is complete.
    Log.d("OverflowMenuDiag", "Uninstall: context runtime type=${context::class.qualifiedName}")
    Log.d("OverflowMenuDiag", "Uninstall: creating intent, package=$packageName")
    val intent = Intent(Intent.ACTION_DELETE).apply {
        data = Uri.fromParts("package", packageName, null)
        // Best-reasoned fix, not a fully confirmed root cause the way
        // the Ignore fix was — flagged explicitly as such. The reported
        // symptom (startActivity() returns normally, no exception, but
        // the system uninstall confirmation screen never appears) is a
        // known, documented pattern when a foreground-launched Intent
        // targets a separate task/activity (here, the system package
        // installer, a different app entirely) without this flag —
        // Android can silently decline to bring the new activity forward
        // rather than throwing. This project's MainActivity is a plain,
        // unwrapped ComponentActivity (verified directly, not assumed)
        // so a missing task flag — not a wrapped or invalid Context — is
        // the most likely explanation matching the exact symptom
        // reported. If this alone doesn't resolve it, the context
        // runtime type logged above is the next thing to check.
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    // DIAGNOSTIC (Sprint 32.3) — temporary, remove before release. Added
    // specifically to compare a working package against a failing one,
    // per real-device testing that showed FLAG_ACTIVITY_NEW_TASK alone
    // (Sprint 32.1) does not resolve this for every package. Every
    // PackageManager call here is purely read-only diagnostic
    // information gathering — none of it affects packageName, intent, or
    // the existing startActivity() call below in any way, and each is
    // independently try/caught so a failure gathering ONE diagnostic
    // (e.g. the target package genuinely not being installed) can never
    // prevent the real, unchanged uninstall flow underneath this logging
    // from running exactly as it already does.
    val packageManager = context.packageManager

    try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        Log.d("OverflowMenuDiag", "Uninstall: getApplicationInfo() succeeded for package=$packageName")
        Log.d(
            "OverflowMenuDiag",
            "Uninstall: applicationInfo.flags=${appInfo.flags} (0x${appInfo.flags.toString(16)})",
        )
        Log.d("OverflowMenuDiag", "Uninstall: applicationInfo.enabled=${appInfo.enabled}")
        Log.d("OverflowMenuDiag", "Uninstall: applicationInfo.sourceDir=${appInfo.sourceDir}")
    } catch (e: PackageManager.NameNotFoundException) {
        Log.d(
            "OverflowMenuDiag",
            "Uninstall: getApplicationInfo() threw NameNotFoundException for package=$packageName",
        )
    }

    try {
        @Suppress("DEPRECATION")
        val installerPackageName = packageManager.getInstallerPackageName(packageName)
        Log.d("OverflowMenuDiag", "Uninstall: installer package=$installerPackageName")
    } catch (e: IllegalArgumentException) {
        Log.d("OverflowMenuDiag", "Uninstall: getInstallerPackageName() threw IllegalArgumentException")
    }

    val resolvedActivity = intent.resolveActivity(packageManager)
    Log.d("OverflowMenuDiag", "Uninstall: intent.resolveActivity()=$resolvedActivity")

    Log.d("OverflowMenuDiag", "Uninstall: complete intent URI=${intent.toUri(Intent.URI_INTENT_SCHEME)}")

    // DIAGNOSTIC (Sprint 32.4) — temporary, remove before release. The
    // existing startActivity() call itself was already known not to
    // throw (Sprint 32.1's own diagnostic logging already confirmed the
    // line after it always ran) — this makes that explicit and
    // permanent-until-removed rather than inferred from "the next log
    // line appeared." Catching the broad Exception type here, not a
    // narrower one, is deliberate: the goal is visibility into whatever
    // startActivity() actually does, not a guess at which specific
    // exception type it might throw. Behavior is otherwise completely
    // unchanged — nothing about the actual uninstall request changes
    // based on which branch runs; both branches are pure logging.
    Log.d("OverflowMenuDiag", "Uninstall: calling startActivity() (existing mechanism, not an ActivityResultLauncher)")
    try {
        context.startActivity(intent)
        Log.d("OverflowMenuDiag", "Uninstall: startActivity returned normally")
        // Tiny, deliberate addition: a distinct line immediately after,
        // making the Logcat sequence unambiguous to read at a glance —
        // "calling startActivity()" -> "startActivity returned
        // normally" -> "Returned from startActivity()" -> whichever
        // MainActivity lifecycle callbacks fire next (or don't).
        Log.d("Uninstall", "Returned from startActivity()")
    } catch (e: Exception) {
        Log.e("OverflowMenuDiag", "Uninstall: startActivity failed", e)
    }
}
