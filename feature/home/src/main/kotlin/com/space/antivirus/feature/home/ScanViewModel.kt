package com.space.antivirus.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanProgress
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.domain.usecase.GetActiveScanSessionUseCase
import com.space.antivirus.domain.usecase.ObserveScanProgressUseCase
import com.space.antivirus.domain.usecase.RunScanRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * This sprint's real architectural finding, documented in full in ADR
 * 0033: RunScanRequestUseCase (Sprint 007+) is a single suspend call that
 * doesn't expose its session id until the whole scan finishes — but
 * observing live progress (ObserveScanProgressUseCase) needs that id
 * immediately. There's no clean, single reactive path from "start a
 * scan" to "watch its progress" in the existing domain API. Rather than
 * change RunScanRequestUseCase's signature (a real, invasive change to
 * heavily-tested orchestration code spanning Sprints 005-016, out of
 * proportion for a UI sprint), this ViewModel bridges the gap with a
 * short, bounded poll for the session GetActiveScanSessionUseCase
 * reports becoming active, immediately after triggering the scan. Session
 * creation is a single Room insert very early in RunScanRequestUseCase's
 * execution, so the poll window is generous relative to how long it
 * actually takes; if the scan fails before ever creating a session (or
 * is rejected by ADR 0020's concurrent-scan guard), the poll simply times
 * out and progress observation is skipped — the scan's own result still
 * reaches the UI normally.
 *
 * Deliberately does NOT model a "Completed" results-detail state beyond a
 * brief summary — Security Center (Sprint 019) already reactively shows
 * full per-threat detail from the same ObserveScanHistoryUseCase this
 * scan persists into; duplicating that here would be redundant UI, not
 * additional value.
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val runScanRequest: RunScanRequestUseCase,
    private val getActiveScanSession: GetActiveScanSessionUseCase,
    private val observeScanProgress: ObserveScanProgressUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun startScan() {
        // Guard against double-trigger — RunScanRequestUseCase itself
        // also guards against concurrent scans (ADR 0020), so this is
        // defense in depth, not the only safeguard.
        if (_uiState.value is ScanUiState.Running) return

        viewModelScope.launch {
            _uiState.value = ScanUiState.Running(progress = null)

            val scanDeferred = async { runScanRequest(defaultScanRequest()) }

            val progressJob = launch {
                val session = awaitActiveSession()
                if (session != null) {
                    observeScanProgress(session.id).collect { progress ->
                        _uiState.value = ScanUiState.Running(progress = progress)
                    }
                }
            }

            val result = scanDeferred.await()
            progressJob.cancel()

            _uiState.value = when (result) {
                is AppResult.Success -> {
                    val scanResult = result.data
                    ScanUiState.Completed(
                        isClean = scanResult.isClean,
                        threatsFound = scanResult.threats.size,
                        itemsScanned = scanResult.statistics.itemsScanned,
                    )
                }
                is AppResult.Failure -> ScanUiState.Error(messageFor(result.error))
                AppResult.Loading -> ScanUiState.Error("Something went wrong starting the scan.")
            }
        }
    }

    /** Returns to Idle from Completed/Error — called when the user
     *  dismisses the inline result banner, or taps Scan Now again. */
    fun acknowledgeResult() {
        if (_uiState.value !is ScanUiState.Running) {
            _uiState.value = ScanUiState.Idle
        }
    }

    private suspend fun awaitActiveSession(): ScanSession? {
        repeat(MAX_SESSION_POLL_ATTEMPTS) {
            val session = (getActiveScanSession() as? AppResult.Success)?.data
            if (session != null) return session
            delay(SESSION_POLL_INTERVAL_MILLIS)
        }
        return null
    }

    private fun defaultScanRequest(): ScanRequest = ScanRequest(
        id = UUID.randomUUID().toString(),
        scanType = ScanType.QUICK,
        // InstalledApplications only — the only scope any real
        // ThreatAnalyzer (Sprints 014/015) can actually evaluate.
        // Scoping to what's genuinely analyzed, not what's enumerable,
        // avoids an honest-but-useless scan that walks files no analyzer
        // will ever look at.
        scopes = listOf(ScanScope.InstalledApplications),
        createdAtEpochMillis = System.currentTimeMillis(),
    )

    private fun messageFor(error: AppError): String = when (error) {
        is AppError.ScanAlreadyInProgress -> "A scan is already running."
        is AppError.PermissionMissing -> "Space Antivirus needs permission to see installed apps to run a scan."
        else -> "Something went wrong during the scan. Please try again."
    }

    private companion object {
        const val MAX_SESSION_POLL_ATTEMPTS = 20
        const val SESSION_POLL_INTERVAL_MILLIS = 50L
    }
}

sealed interface ScanUiState {
    data object Idle : ScanUiState

    /** progress is null in the brief window between triggering the scan
     *  and either the session becoming observable or its first real
     *  ScanProgress snapshot arriving — rendered as an indeterminate
     *  wait, not a stalled determinate one. */
    data class Running(val progress: ScanProgress?) : ScanUiState

    data class Completed(
        val isClean: Boolean,
        val threatsFound: Int,
        val itemsScanned: Int,
    ) : ScanUiState

    data class Error(val message: String) : ScanUiState
}
