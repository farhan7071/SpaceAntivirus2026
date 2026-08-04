package com.space.antivirus.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.model.CleanupRecord
import com.space.antivirus.core.model.ProtectionState
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.domain.usecase.ObserveCleanupHistoryUseCase
import com.space.antivirus.domain.usecase.ObserveProtectionStateUseCase
import com.space.antivirus.domain.usecase.ObserveScanHistoryUseCase
import com.space.antivirus.domain.usecase.ObserveTrustedItemsUseCase
import com.space.antivirus.domain.usecase.SetProtectionEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
 * Sprint 040 added a third source, ObserveCleanupHistoryUseCase, for
 * the same reason and in the same shape: the Cleaner began persisting
 * real cleanup records in Sprint 039, and Home's Recent Activity had
 * been showing only half the story since. It's a Flow, so it composes
 * into the existing combine() rather than needing a one-shot read —
 * Recent Activity updates the moment a cleanup finishes while Home is
 * open. GetLastCleanupUseCase exists and is deliberately NOT used here,
 * exactly as GetLatestScanResultUseCase isn't, for that same reason.
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
    observeCleanupHistory: ObserveCleanupHistoryUseCase,
    observeProtectionState: ObserveProtectionStateUseCase,
    private val setProtectionEnabled: SetProtectionEnabledUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        observeScanHistory(),
        observeTrustedItems(),
        observeCleanupHistory(),
        observeProtectionState(),
    ) { scanHistory, trustedItems, cleanupHistory, protection ->
        val lastScan = scanHistory.firstOrNull()
        // Cast to the sealed supertype explicitly — without it, this
        // lambda's inferred return type is HomeUiState.Loaded specifically,
        // and the .catch{} below (which emits a sibling HomeUiState.Error)
        // would fail to type-check against a Flow<HomeUiState.Loaded>.
        HomeUiState.Loaded(
            protectionStatus = protectionStatusFor(lastScan),
            lastScanSummary = lastScan?.toSummary(),
            trustedItemsCount = trustedItems.size,
            lastCleanupSummary = cleanupHistory.firstOrNull()?.toSummary(),
            protection = protection,
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

    /** A cancelled cleanup is still a real cleanup — the files deleted
     *  before the user pressed Stop are genuinely gone — so it appears in
     *  Recent Activity like any other, and carries the flag so the screen
     *  can say it was stopped early rather than implying it finished. */
    private fun CleanupRecord.toSummary(): LastCleanupSummary = LastCleanupSummary(
        bytesFreed = bytesFreed,
        itemsDeleted = itemsDeleted,
        cleanedAtEpochMillis = completedAtEpochMillis,
        wasCancelled = wasCancelled,
    )

    /**
     * Sprint 042 — Home's quick toggle.
     *
     * Calls the same use case Settings' switch does, so the two cannot
     * diverge: all the schedule-then-persist-then-notify ordering lives
     * in ProtectionManager. A failure leaves the persisted state
     * untouched, so the switch simply springs back — the state Home
     * renders is always what WorkManager really has.
     */
    fun onProtectionToggled(enabled: Boolean) {
        viewModelScope.launch { setProtectionEnabled(enabled) }
    }

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
        /** Null until the user has actually run a cleanup. Absent, never
         *  a placeholder zero. */
        val lastCleanupSummary: LastCleanupSummary? = null,
        /** Sprint 042. Null only until the first emission arrives. */
        val protection: ProtectionState? = null,
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

data class LastCleanupSummary(
    val bytesFreed: Long,
    val itemsDeleted: Int,
    val cleanedAtEpochMillis: Long,
    val wasCancelled: Boolean,
)
