package com.space.antivirus.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.model.ScanProgress
import com.space.antivirus.core.ui.component.AppCard
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppFilledButton
import com.space.antivirus.core.ui.component.AppLinearProgress
import com.space.antivirus.core.ui.component.AppTextButton
import com.space.antivirus.core.ui.component.Severity
import com.space.antivirus.core.ui.component.StatusChip
import java.text.DateFormat
import java.util.Date

/** Exposed so HomeScreenTest can reliably find the loading indicator —
 *  there's no visible text to assert on in the Loading state otherwise. */
const val HOME_LOADING_TEST_TAG = "home_loading_indicator"

/**
 * This project's first production screen (Sprint 017) — establishes the
 * stateful/stateless split every remaining feature screen follows:
 * HomeRoute collects ViewModel state (the only place that touches
 * hiltViewModel()/collectAsStateWithLifecycle), HomeScreen is a pure
 * function of HomeUiState (+ ScanUiState as of Sprint 020) with no
 * ViewModel/DI awareness at all.
 *
 * Sprint 020: the Scan Now button becomes real, driven by a SEPARATE
 * ScanViewModel (ADR 0033) rather than folded into HomeViewModel — active
 * scan orchestration and passive protection-status observation are
 * different concerns of the same screen, each independently testable.
 * Deliberately does NOT show per-threat completion detail here — Security
 * Center (Sprint 019) already reactively shows that from the same
 * underlying data this scan persists into; this screen shows only a
 * brief completion summary.
 */
@Composable
fun HomeRoute(
    homeViewModel: HomeViewModel = hiltViewModel(),
    scanViewModel: ScanViewModel = hiltViewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val scanState by scanViewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        scanState = scanState,
        onScanClick = scanViewModel::startScan,
        onAcknowledgeScanResult = scanViewModel::acknowledgeResult,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    scanState: ScanUiState,
    onScanClick: () -> Unit,
    onAcknowledgeScanResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is HomeUiState.Loading -> HomeLoading(modifier)
        is HomeUiState.Loaded -> HomeLoaded(uiState, scanState, onScanClick, onAcknowledgeScanResult, modifier)
        is HomeUiState.Error -> HomeError(uiState, modifier)
    }
}

@Composable
private fun HomeLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppCircularProgress(progress = null, modifier = Modifier.testTag(HOME_LOADING_TEST_TAG))
    }
}

@Composable
private fun HomeError(state: HomeUiState.Error, modifier: Modifier = Modifier) {
    AppEmptyState(
        icon = Icons.Default.Warning,
        message = state.message,
        modifier = modifier,
    )
}

@Composable
private fun HomeLoaded(
    state: HomeUiState.Loaded,
    scanState: ScanUiState,
    onScanClick: () -> Unit,
    onAcknowledgeScanResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        ProtectionStatusCard(state.protectionStatus, state.lastScanSummary)
        LastScanCard(state.lastScanSummary)
        TrustedItemsCard(state.trustedItemsCount)
        ScanActionSection(scanState, onScanClick, onAcknowledgeScanResult)
    }
}

@Composable
private fun ProtectionStatusCard(status: ProtectionStatus, lastScan: LastScanSummary?) {
    val (headline, supportingText) = when (status) {
        ProtectionStatus.PROTECTED ->
            "You're protected" to "No threats found in your last scan"
        ProtectionStatus.NEEDS_ATTENTION ->
            "Attention needed" to
                "${lastScan?.threatsFound ?: 0} item(s) from your last scan are worth reviewing"
        ProtectionStatus.UNKNOWN ->
            "Protection status unknown" to "Run your first scan to see your protection status"
    }

    AppCard(headline = headline, supportingText = supportingText) {
        when (status) {
            ProtectionStatus.PROTECTED -> StatusChip(Severity.INFO)
            ProtectionStatus.NEEDS_ATTENTION -> StatusChip(Severity.ATTENTION)
            ProtectionStatus.UNKNOWN -> Unit // nothing to grade yet — no chip
        }
    }
}

@Composable
private fun LastScanCard(lastScan: LastScanSummary?) {
    if (lastScan == null) {
        AppCard(headline = "No scans yet", supportingText = "Run a scan to see results here")
        return
    }

    val formattedDate = remember(lastScan.scannedAtEpochMillis) {
        DateFormat.getDateTimeInstance().format(Date(lastScan.scannedAtEpochMillis))
    }
    val resultText = if (lastScan.isClean) {
        "No threats found"
    } else {
        "${lastScan.threatsFound} item(s) found"
    }

    AppCard(headline = "Last scan", supportingText = "$formattedDate \u00B7 $resultText")
}

@Composable
private fun TrustedItemsCard(trustedItemsCount: Int) {
    val supportingText = if (trustedItemsCount == 1) {
        "1 item trusted"
    } else {
        "$trustedItemsCount items trusted"
    }
    AppCard(headline = "Trusted Items", supportingText = supportingText)
}

@Composable
private fun ScanActionSection(
    scanState: ScanUiState,
    onScanClick: () -> Unit,
    onAcknowledgeScanResult: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        when (scanState) {
            is ScanUiState.Completed -> ScanResultBanner(
                message = if (scanState.isClean) {
                    "Scan complete — no threats found (${scanState.itemsScanned} apps checked)."
                } else {
                    "Scan complete — ${scanState.threatsFound} item(s) found. " +
                        "See Security Center for details."
                },
                onDismiss = onAcknowledgeScanResult,
            )
            is ScanUiState.Error -> ScanResultBanner(message = scanState.message, onDismiss = onAcknowledgeScanResult)
            else -> Unit
        }

        if (scanState is ScanUiState.Running) {
            ScanProgressIndicator(scanState.progress)
        } else {
            AppFilledButton(text = "Scan Now", onClick = onScanClick, enabled = true)
        }
    }
}

@Composable
private fun ScanResultBanner(message: String, onDismiss: () -> Unit) {
    AppCard(headline = "Scan Result", supportingText = message) {
        AppTextButton(text = "Dismiss", onClick = onDismiss)
    }
}

@Composable
private fun ScanProgressIndicator(progress: ScanProgress?) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        val fraction = if (progress != null && progress.totalItems > 0) {
            progress.itemsProcessed.toFloat() / progress.totalItems.toFloat()
        } else {
            null
        }
        AppLinearProgress(progress = fraction, modifier = Modifier.fillMaxWidth())
        val label = if (progress != null && progress.totalItems > 0) {
            "Scanning\u2026 ${progress.itemsProcessed} of ${progress.totalItems}"
        } else {
            "Starting scan\u2026"
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
