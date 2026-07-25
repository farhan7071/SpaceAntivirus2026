package com.space.antivirus.feature.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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

/**
 * Follows ADR 0030's stateful/stateless split exactly, same as Home
 * (Sprint 017) and Onboarding (Sprint 018). Deliberately uses only
 * Icons.Default.Warning — the one icon Sprint 017's verification
 * confirmed is genuinely part of this project's baseline (non-Extended)
 * Material icon set. No other icon is introduced, per ADR 0031's standing
 * caution about guessing at icon availability without a real compiler.
 */
@Composable
fun SecurityCenterRoute(
    viewModel: SecurityCenterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SecurityCenterScreen(uiState = uiState)
}

@Composable
fun SecurityCenterScreen(uiState: SecurityCenterUiState, modifier: Modifier = Modifier) {
    when (uiState) {
        is SecurityCenterUiState.Loading -> SecurityCenterLoading(modifier)
        is SecurityCenterUiState.Loaded -> SecurityCenterLoaded(uiState, modifier)
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
private fun SecurityCenterLoaded(state: SecurityCenterUiState.Loaded, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current

    when {
        state.protectionStatus == ProtectionStatus.UNKNOWN -> AppEmptyState(
            icon = Icons.Default.Warning,
            message = "No scan results yet. Run a scan from Home to see your security status here.",
            modifier = modifier.fillMaxSize(),
        )
        state.threats.isEmpty() -> AppEmptyState(
            icon = Icons.Default.Warning,
            message = "No threats found. Your last scan didn't detect anything to review.",
            modifier = modifier.fillMaxSize(),
        )
        else -> LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            items(state.threats) { threat -> ThreatCard(threat) }
        }
    }
}

@Composable
private fun ThreatCard(threat: ThreatSummary) {
    AppCard(headline = threat.title, supportingText = threat.description) {
        StatusChip(threat.riskLevel.toSeverity())
    }
}

private fun RiskLevel.toSeverity(): Severity = when (this) {
    RiskLevel.INFO -> Severity.INFO
    RiskLevel.ATTENTION -> Severity.ATTENTION
    RiskLevel.ACTION_NEEDED -> Severity.ACTION_NEEDED
}
