package com.space.antivirus.feature.clean

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.space.antivirus.core.model.CleanableCategory
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.ui.component.AppCard
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppFilledButton

/**
 * Replaces the Sprint 003 placeholder — the last genuinely outstanding
 * Phase C screen. Follows ADR 0030's stateful/stateless split exactly,
 * same as every prior feature screen. Only Icons.Default.Warning used —
 * the one icon confirmed genuinely baseline-safe since Sprint 017's
 * verification; no other icon is introduced.
 */
@Composable
fun CleanRoute(
    viewModel: CleanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CleanScreen(uiState = uiState, onScanClick = viewModel::scanForJunk)
}

@Composable
fun CleanScreen(uiState: CleanUiState, onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    when (uiState) {
        is CleanUiState.Idle -> CleanIdle(onScanClick, modifier)
        is CleanUiState.Loading -> CleanLoading(modifier)
        is CleanUiState.Loaded -> CleanLoaded(uiState, onScanClick, modifier)
        is CleanUiState.Error -> CleanError(uiState, onScanClick, modifier)
    }
}

@Composable
private fun CleanIdle(onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.medium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Scan for cache, temporary, and log files you may not need anymore.",
            style = MaterialTheme.typography.bodyLarge,
        )
        AppFilledButton(
            text = "Scan for Junk Files",
            onClick = onScanClick,
            modifier = Modifier.padding(top = spacing.medium),
        )
    }
}

@Composable
private fun CleanLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppCircularProgress(progress = null)
    }
}

@Composable
private fun CleanError(state: CleanUiState.Error, onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppEmptyState(icon = Icons.Default.Warning, message = state.message)
        AppFilledButton(
            text = "Try Again",
            onClick = onScanClick,
            modifier = Modifier.padding(top = spacing.medium),
        )
    }
}

@Composable
private fun CleanLoaded(state: CleanUiState.Loaded, onScanClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current

    if (state.items.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppEmptyState(
                icon = Icons.Default.Warning,
                message = "No junk files found. Your storage looks clean.",
            )
            AppFilledButton(
                text = "Scan Again",
                onClick = onScanClick,
                modifier = Modifier.padding(top = spacing.medium),
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppCard(
            headline = formatSize(state.totalSizeBytes) + " reclaimable",
            supportingText = "${state.items.size} item(s) found",
            modifier = Modifier.padding(spacing.medium),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            items(state.items) { item -> CleanableItemCard(item) }
        }
        AppFilledButton(
            text = "Scan Again",
            onClick = onScanClick,
            modifier = Modifier.padding(spacing.medium),
        )
    }
}

@Composable
private fun CleanableItemCard(item: CleanableItem) {
    AppCard(
        headline = item.name,
        supportingText = "${item.category.toDisplayLabel()} \u00B7 ${formatSize(item.sizeBytes)} \u00B7 ${item.reason}",
    )
}

private fun CleanableCategory.toDisplayLabel(): String = when (this) {
    CleanableCategory.CACHE_FILE -> "Cache file"
    CleanableCategory.TEMPORARY_FILE -> "Temporary file"
    CleanableCategory.LOG_FILE -> "Log file"
    CleanableCategory.LEFTOVER_INSTALLER -> "Leftover installer"
}

/** Presentation-layer formatting only (same precedent as HomeScreen's
 *  date formatting, HistoryScreen's duration formatting) — not business
 *  logic, so it stays in the Screen file, not the ViewModel. */
private fun formatSize(sizeBytes: Long): String = when {
    sizeBytes >= 1_000_000 -> "%.1f MB".format(sizeBytes / 1_000_000.0)
    sizeBytes >= 1_000 -> "%.1f KB".format(sizeBytes / 1_000.0)
    else -> "$sizeBytes B"
}
