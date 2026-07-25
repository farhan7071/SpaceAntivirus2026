package com.space.antivirus.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.domain.usecase.ObserveScanHistoryUseCase
import com.space.antivirus.domain.usecase.ObserveTrustedItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * This project's first production ViewModel — establishes the pattern
 * every remaining feature screen follows (Sprint 017, per this sprint's
 * own "establish the UI architecture" objective).
 *
 * REACTIVE BY DESIGN, not just by requirement: both data sources are
 * Flow-based (ObserveScanHistoryUseCase, ObserveTrustedItemsUseCase),
 * combined and exposed as a single lifecycle-aware StateFlow via
 * stateIn(WhileSubscribed(5_000)) — the standard current Android
 * architecture pattern: upstream collection stops when there's no UI
 * observer, survives brief configuration-change gaps without
 * re-subscribing immediately. GetLatestScanResultUseCase and
 * GetActiveScanSessionUseCase both exist but are deliberately NOT used
 * here — they're one-shot suspend calls, and "last scan" is more
 * consistently reactive when derived from ObserveScanHistoryUseCase's
 * own Flow (updates live the moment a new scan completes while Home is
 * open, not just on next ViewModel creation). See docs/architecture.md.
 *
 * Active-scan-session state (whether a scan is CURRENTLY running,
 * independent of HomeViewModel's own passive observation) is deliberately
 * NOT surfaced by this ViewModel — that's ScanViewModel's job (Sprint
 * 020, ADR 0033), a separate ViewModel for the separate concern of active
 * scan orchestration. HomeViewModel stays focused on passively reflecting
 * whatever the latest persisted scan history/trusted items say.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeScanHistory: ObserveScanHistoryUseCase,
    observeTrustedItems: ObserveTrustedItemsUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        observeScanHistory(),
        observeTrustedItems(),
    ) { scanHistory, trustedItems ->
        val lastScan = scanHistory.firstOrNull()
        // Cast to the sealed supertype explicitly — without it, this
        // lambda's inferred return type is HomeUiState.Loaded specifically,
        // and the .catch{} below (which emits a sibling HomeUiState.Error)
        // would fail to type-check against a Flow<HomeUiState.Loaded>.
        HomeUiState.Loaded(
            protectionStatus = protectionStatusFor(lastScan),
            lastScanSummary = lastScan?.toSummary(),
            trustedItemsCount = trustedItems.size,
        ) as HomeUiState
    }
        .catch { error ->
            emit(HomeUiState.Error(error.message ?: "Something went wrong loading your protection status."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState.Loading,
        )

    private fun protectionStatusFor(lastScan: ScanResult?): ProtectionStatus = when {
        lastScan == null -> ProtectionStatus.UNKNOWN
        // observeScanHistory() is only ever expected to return COMPLETED
        // sessions (the real repository's own query already filters this
        // way — Sprint 010) but a ScanResult's type doesn't GUARANTEE
        // that, so this stays a defensive check rather than an assumption
        // baked silently into the branch below.
        lastScan.session.state != ScanSessionState.COMPLETED -> ProtectionStatus.UNKNOWN
        lastScan.isClean -> ProtectionStatus.PROTECTED
        else -> ProtectionStatus.NEEDS_ATTENTION
    }

    private fun ScanResult.toSummary(): LastScanSummary = LastScanSummary(
        isClean = isClean,
        threatsFound = threats.size,
        scannedAtEpochMillis = session.completedAtEpochMillis ?: session.startedAtEpochMillis,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Loaded(
        val protectionStatus: ProtectionStatus,
        val lastScanSummary: LastScanSummary?,
        val trustedItemsCount: Int,
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

/** UNKNOWN covers both "never scanned" and "last scan didn't complete" —
 *  both are genuinely "we don't know," not a lesser form of PROTECTED. */
enum class ProtectionStatus {
    PROTECTED,
    NEEDS_ATTENTION,
    UNKNOWN,
}

data class LastScanSummary(
    val isClean: Boolean,
    val threatsFound: Int,
    val scannedAtEpochMillis: Long,
)
