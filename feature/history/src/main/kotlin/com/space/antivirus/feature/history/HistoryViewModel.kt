package com.space.antivirus.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.Threat
import com.space.antivirus.core.model.TrustedItemType
import com.space.antivirus.domain.reporting.ThreatDescriptionProvider
import com.space.antivirus.domain.usecase.AddTrustedItemParams
import com.space.antivirus.domain.usecase.AddTrustedItemUseCase
import com.space.antivirus.domain.usecase.ObserveScanHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Replaces the Sprint 003 placeholder. Follows ADR 0030's pattern
 * exactly. Reuses ObserveScanHistoryUseCase directly — the same Flow
 * HomeViewModel and SecurityCenterViewModel already use, but unmapped to
 * just the latest entry: History shows every completed scan the
 * repository has, in the same most-recent-first order Sprint 010's
 * underlying query already provides.
 *
 * Sprint 030 (ADR 0044): ThreatSummary restructured to match
 * SecurityCenterViewModel's identical new shape (shortSummary/
 * technicalDetail/evidenceBullets/recommendation, replacing the old
 * title/description pair) — both screens now render the same shared
 * ThreatSummaryCard (core:ui). Still a genuinely separate local type,
 * not a shared one — the same rule-of-three reasoning ADR 0032 already
 * established still applies; this is the second occurrence, not the
 * third. Gained ThreatDescriptionProvider (for shortSummaryFor/
 * recommendationFor) and AddTrustedItemUseCase (for a real onIgnoreClick,
 * mirroring SecurityCenterViewModel's identical addition) as new
 * dependencies for the same reasons documented there.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeScanHistory: ObserveScanHistoryUseCase,
    private val descriptionProvider: ThreatDescriptionProvider,
    private val addTrustedItem: AddTrustedItemUseCase,
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

    /** Same fire-and-forget reasoning as SecurityCenterViewModel's
     *  identical method — see that class's KDoc for the full rationale. */
    fun onIgnoreClick(packageName: String) {
        viewModelScope.launch {
            addTrustedItem(
                AddTrustedItemParams(
                    identifier = packageName,
                    type = TrustedItemType.APPLICATION,
                    reason = "Ignored from History",
                ),
            )
        }
    }

    private fun ScanResult.toEntry(): ScanHistoryEntry = ScanHistoryEntry(
        sessionId = session.id,
        completedAtEpochMillis = session.completedAtEpochMillis ?: session.startedAtEpochMillis,
        durationMillis = statistics.durationMillis,
        itemsScanned = statistics.itemsScanned,
        isClean = isClean,
        threats = threats.map { it.toSummary() },
    )

    private fun Threat.toSummary(): ThreatSummary = ThreatSummary(
        appLabel = appLabel.ifBlank { targetIdentifier },
        packageName = targetIdentifier,
        riskLevel = riskLevel,
        shortSummary = descriptionProvider.shortSummaryFor(detections),
        technicalDetail = description,
        evidenceBullets = detections.map { it.evidenceDescription },
        recommendation = descriptionProvider.recommendationFor(threatType, detections, riskLevel),
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

/** Sprint 030 (ADR 0044) — see SecurityCenterViewModel.ThreatSummary's
 *  own KDoc for the full reasoning; this is the identical shape, kept
 *  local rather than shared per the rule-of-three note above. */
data class ThreatSummary(
    val appLabel: String,
    val packageName: String,
    val riskLevel: RiskLevel,
    val shortSummary: String,
    val technicalDetail: String,
    val evidenceBullets: List<String>,
    val recommendation: String,
)
