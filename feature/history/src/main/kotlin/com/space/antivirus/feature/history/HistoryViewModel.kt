package com.space.antivirus.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.Threat
import com.space.antivirus.domain.usecase.ObserveScanHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Replaces the Sprint 003 placeholder. Follows ADR 0030's pattern
 * exactly. Reuses ObserveScanHistoryUseCase directly — the same Flow
 * HomeViewModel and SecurityCenterViewModel already use, but unmapped to
 * just the latest entry: History shows every completed scan the
 * repository has, in the same most-recent-first order Sprint 010's
 * underlying query already provides.
 *
 * ThreatSummary is duplicated from SecurityCenterViewModel's identical
 * shape, same rule-of-three reasoning as ADR 0032 — this is the second
 * occurrence, not the third, so it stays local to this feature module.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeScanHistory: ObserveScanHistoryUseCase,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = observeScanHistory()
        .map { scanResults ->
            HistoryUiState.Loaded(entries = scanResults.map { it.toEntry() }) as HistoryUiState
        }
        .catch { error ->
            emit(HistoryUiState.Error(error.message ?: "Something went wrong loading your scan history."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HistoryUiState.Loading,
        )

    private fun ScanResult.toEntry(): ScanHistoryEntry = ScanHistoryEntry(
        sessionId = session.id,
        completedAtEpochMillis = session.completedAtEpochMillis ?: session.startedAtEpochMillis,
        durationMillis = statistics.durationMillis,
        itemsScanned = statistics.itemsScanned,
        isClean = isClean,
        threats = threats.map { it.toSummary() },
    )

    private fun Threat.toSummary(): ThreatSummary = ThreatSummary(
        title = title,
        description = description,
        riskLevel = riskLevel,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data class Loaded(val entries: List<ScanHistoryEntry>) : HistoryUiState

    data class Error(val message: String) : HistoryUiState
}

data class ScanHistoryEntry(
    val sessionId: String,
    val completedAtEpochMillis: Long,
    val durationMillis: Long,
    val itemsScanned: Int,
    val isClean: Boolean,
    val threats: List<ThreatSummary>,
)

data class ThreatSummary(
    val title: String,
    val description: String,
    val riskLevel: RiskLevel,
)
