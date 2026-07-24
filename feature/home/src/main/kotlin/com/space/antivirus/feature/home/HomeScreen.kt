package com.space.antivirus.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.ui.component.AppCard
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppFilledButton
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
 * function of HomeUiState with no ViewModel/DI awareness at all — easily
 * previewable, easily UI-tested with a hand-built state, and physically
 * incapable of hiding business logic since it has no way to reach any.
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState)
}

@Composable
fun HomeScreen(uiState: HomeUiState, modifier: Modifier = Modifier) {
    when (uiState) {
        is HomeUiState.Loading -> HomeLoading(modifier)
        is HomeUiState.Loaded -> HomeLoaded(uiState, modifier)
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
        icon = Icons.Default.ErrorOutline,
        message = state.message,
        modifier = modifier,
    )
}

@Composable
private fun HomeLoaded(state: HomeUiState.Loaded, modifier: Modifier = Modifier) {
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
        AppFilledButton(
            text = "Scan Now",
            onClick = {},
            // Scan execution/progress UI is explicitly out of scope for
            // this sprint (Phase C, first UI screen) — this is the
            // "disabled or placeholder action" this sprint's own brief
            // said was acceptable, not an oversight.
            enabled = false,
        )
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
