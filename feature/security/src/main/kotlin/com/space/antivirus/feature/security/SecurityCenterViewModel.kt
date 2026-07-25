package com.space.antivirus.feature.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSessionState
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
 */
@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    observeScanHistory: ObserveScanHistoryUseCase,
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
        title = title,
        description = description,
        riskLevel = riskLevel,
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

data class ThreatSummary(
    val title: String,
    val description: String,
    val riskLevel: RiskLevel,
)
