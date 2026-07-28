package com.space.antivirus.feature.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSessionState
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
 * Replaces the Sprint 003 placeholder. Follows ADR 0030's established
 * pattern exactly: reactive Flow-based state, stateIn(WhileSubscribed),
 * ViewModel exposes only domain types (RiskLevel, not core:ui's Severity)
 * so it stays UI-toolkit-agnostic — the RiskLevel -> Severity mapping,
 * and now the evidence-text -> EvidenceIcon mapping too, both live in
 * SecurityCenterScreen.kt, not here.
 *
 * Deliberately reuses ObserveScanHistoryUseCase — the same UseCase
 * HomeViewModel already uses — rather than adding a new one. Home
 * surfaces only a compact summary of the latest scan; this screen
 * surfaces the full Threat list from that same latest scan, which is
 * exactly the additional detail Sprint 019's brief asked this screen to
 * add without duplicating what Home already shows.
 *
 * Trusted items are deliberately NOT shown here (unlike Home) — that
 * summary already lives on Home, and repeating it here wouldn't add any
 * security-specific value; this screen's distinct purpose is threat
 * detail, not a second home-page.
 *
 * Sprint 030: gained AddTrustedItemUseCase and a real onIgnoreClick
 * handler — "Ignore" reuses AddTrustedItemUseCase (Sprint 008), never
 * previously wired to any UI anywhere in this project. Not a new
 * mechanism; finally connecting one that already existed. "Open app
 * info" and "Uninstall" are deliberately NOT handled here — both are
 * pure Android Intent-launching concerns needing a Context, which a
 * ViewModel should never hold a reference to; SecurityCenterScreen.kt
 * launches those directly via LocalContext.current, the same separation
 * every prior screen in this project has kept between ViewModel state
 * and Android-framework-specific UI actions.
 */
@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    observeScanHistory: ObserveScanHistoryUseCase,
    private val descriptionProvider: ThreatDescriptionProvider,
    private val addTrustedItem: AddTrustedItemUseCase,
) : ViewModel() {

    val uiState: StateFlow<SecurityCenterUiState> = observeScanHistory()
        .map { scanHistory ->
            val lastScan = scanHistory.firstOrNull()
            // Cast to the sealed supertype explicitly — same reason as
            // HomeViewModel (ADR 0030): without it, this lambda's
            // inferred return type is Loaded specifically, and the
            // .catch{} below (emitting a sibling Error) would not
            // type-check against a Flow<Loaded>.
            SecurityCenterUiState.Loaded(
                protectionStatus = protectionStatusFor(lastScan),
                lastScanCompletedAtEpochMillis = lastScan?.session?.completedAtEpochMillis,
                threats = lastScan?.threats.orEmpty().map { it.toSummary() },
            ) as SecurityCenterUiState
        }
        .catch { error ->
            emit(
                SecurityCenterUiState.Error(
                    error.message ?: "Something went wrong loading your security status.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SecurityCenterUiState.Loading,
        )

    /**
     * Fire-and-forget by design, same as every other one-shot UseCase
     * call from a ViewModel in this project (e.g. ScanViewModel's
     * startScan) — a failure here isn't catastrophic (the item simply
     * isn't marked trusted; the user can try again), and this screen has
     * no dedicated error-banner mechanism to add one to for a single,
     * low-stakes action. A future sprint wanting explicit failure
     * feedback for this specific action has a real, small extension
     * point here, not a redesign.
     */
    fun onIgnoreClick(packageName: String) {
        viewModelScope.launch {
            addTrustedItem(
                AddTrustedItemParams(
                    identifier = packageName,
                    type = TrustedItemType.APPLICATION,
                    reason = "Ignored from Security Center",
                ),
            )
        }
    }

    private fun protectionStatusFor(lastScan: ScanResult?): ProtectionStatus = when {
        lastScan == null -> ProtectionStatus.UNKNOWN
        // Same defensive check as HomeViewModel (ADR 0030) — the real
        // repository's query already filters to COMPLETED only (Sprint
        // 010), but ScanResult's type doesn't itself guarantee that.
        lastScan.session.state != ScanSessionState.COMPLETED -> ProtectionStatus.UNKNOWN
        lastScan.isClean -> ProtectionStatus.PROTECTED
        else -> ProtectionStatus.NEEDS_ATTENTION
    }

    private fun Threat.toSummary(): ThreatSummary = ThreatSummary(
        // Defensive fallback, not expected in practice — every real
        // Threat since Sprint 029 has appLabel populated by
        // BuildThreatUseCase. Falling back to the package name rather
        // than showing a blank headline if it were ever empty.
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

sealed interface SecurityCenterUiState {
    data object Loading : SecurityCenterUiState

    data class Loaded(
        val protectionStatus: ProtectionStatus,
        val lastScanCompletedAtEpochMillis: Long?,
        val threats: List<ThreatSummary>,
    ) : SecurityCenterUiState

    data class Error(val message: String) : SecurityCenterUiState
}

/** Duplicated intentionally from HomeViewModel's identical enum, not
 *  shared — feature modules do not depend on each other in this project
 *  (only :app composes them), and this enum is small enough that a
 *  cross-feature or new shared module for it isn't justified yet. If a
 *  third screen needs the same derivation, that's the point to revisit
 *  this (rule of three), not before. */
enum class ProtectionStatus {
    PROTECTED,
    NEEDS_ATTENTION,
    UNKNOWN,
}

/**
 * Sprint 030 (ADR 0044) — restructured again for the new ThreatSummaryCard
 * (core:ui): `shortSummary` (a single compact sentence, always visible)
 * and `technicalDetail` (Threat.description's long-form prose, shown
 * only when a card is expanded) replace Sprint 029's single, always-
 * visible `reasons` list. `evidenceBullets` (renamed from `reasons`,
 * same data — one line per Detection) also moved into the expanded
 * state. `packageName` stays visible collapsed (goal #4: app identity
 * before any explanation). No EvidenceIcon field here — that mapping is
 * a core:ui presentation concern, derived from evidenceBullets text in
 * SecurityCenterScreen.kt, matching the same layering this ViewModel
 * already used for RiskLevel -> Severity.
 */
data class ThreatSummary(
    val appLabel: String,
    val packageName: String,
    val riskLevel: RiskLevel,
    val shortSummary: String,
    val technicalDetail: String,
    val evidenceBullets: List<String>,
    val recommendation: String,
)
