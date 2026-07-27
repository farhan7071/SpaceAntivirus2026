package com.space.antivirus.feature.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.Threat
import com.space.antivirus.domain.reporting.ThreatDescriptionProvider
import com.space.antivirus.domain.usecase.ObserveScanHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Replaces the Sprint 003 placeholder. Follows ADR 0030's established
 * pattern exactly: reactive Flow-based state, stateIn(WhileSubscribed),
 * ViewModel exposes only domain types (RiskLevel, not core:ui's Severity)
 * so it stays UI-toolkit-agnostic — the RiskLevel -> Severity mapping
 * lives in SecurityCenterScreen.kt, not here.
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
 * Sprint 029: gained ThreatDescriptionProvider as a second dependency —
 * needed to call recommendationFor(threatType) when building each
 * ThreatSummary. This is a real, verified root-cause fix (ADR 0043), not
 * a cosmetic addition: this screen previously showed threat.title (a
 * generic, threatType-derived category label) as its headline instead of
 * the app's actual name, and a single long concatenated description
 * instead of separate evidence bullets — see ThreatSummary's own KDoc.
 */
@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    observeScanHistory: ObserveScanHistoryUseCase,
    private val descriptionProvider: ThreatDescriptionProvider,
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
        reasons = detections.map { it.evidenceDescription },
        recommendation = descriptionProvider.recommendationFor(threatType),
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
 * Sprint 029 (ADR 0043) — restructured for app-identity-first,
 * evidence-grouped display, replacing the prior (title, description)
 * shape. `title` was a generic, threatType-derived category label (e.g.
 * "Unusual permission combination") shown as the card's headline instead
 * of the app's actual name — when different apps shared a threatType,
 * they showed identical headline text, indistinguishable from literal
 * duplication in a list. `description` concatenated every Detection's
 * full evidence text plus generic lead-in/suggested-action prose into
 * one long paragraph. `appLabel` is now the headline; `reasons` (one
 * short line per Detection, unpacked from the same detections list that
 * always existed) render as separate bullets; `recommendation` is a
 * short, separate, threatType-derived action line.
 */
data class ThreatSummary(
    val appLabel: String,
    val packageName: String,
    val riskLevel: RiskLevel,
    val reasons: List<String>,
    val recommendation: String,
)
