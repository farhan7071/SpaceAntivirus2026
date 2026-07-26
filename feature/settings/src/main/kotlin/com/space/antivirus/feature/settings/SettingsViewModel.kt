package com.space.antivirus.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.domain.usecase.CancelBackgroundScanUseCase
import com.space.antivirus.domain.usecase.ObserveBackgroundProtectionEnabledUseCase
import com.space.antivirus.domain.usecase.ObserveLastScheduledAtUseCase
import com.space.antivirus.domain.usecase.ObserveScanIntervalUseCase
import com.space.antivirus.domain.usecase.RecordBackgroundProtectionDisabledUseCase
import com.space.antivirus.domain.usecase.RecordBackgroundProtectionEnabledParams
import com.space.antivirus.domain.usecase.RecordBackgroundProtectionEnabledUseCase
import com.space.antivirus.domain.usecase.ScheduleBackgroundScanUseCase
import com.space.antivirus.domain.usecase.SetScanIntervalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Replaces the Sprint 003 placeholder. Reuses ScheduleBackgroundScanUseCase/
 * CancelBackgroundScanUseCase directly (Sprint 024/025) rather than any
 * new scheduling logic — this screen is the first real caller of both,
 * exactly what ADR 0037/0038 said the next increment would be.
 *
 * Reactive Flow-combine shape (like HomeViewModel/SecurityCenterViewModel),
 * not action-triggered like ScanViewModel/CleanViewModel — the underlying
 * data here (persisted preferences) IS genuinely ongoing, observable
 * state, unlike a one-shot scan or file enumeration.
 *
 * PERSIST-ONLY-ON-CONFIRMED-SUCCESS, throughout: every write to
 * BackgroundProtectionPreferences happens only after the corresponding
 * scheduler call has already succeeded. This is what keeps the persisted
 * state (and therefore lastScheduledAtEpochMillis) an honest reflection
 * of what WorkManager actually has scheduled, rather than an assumption
 * — see BackgroundProtectionPreferences' own KDoc for the same reasoning
 * stated from the contract's side.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeBackgroundProtectionEnabled: ObserveBackgroundProtectionEnabledUseCase,
    observeScanInterval: ObserveScanIntervalUseCase,
    observeLastScheduledAt: ObserveLastScheduledAtUseCase,
    private val scheduleBackgroundScan: ScheduleBackgroundScanUseCase,
    private val cancelBackgroundScan: CancelBackgroundScanUseCase,
    private val recordEnabled: RecordBackgroundProtectionEnabledUseCase,
    private val recordDisabled: RecordBackgroundProtectionDisabledUseCase,
    private val setScanInterval: SetScanIntervalUseCase,
) : ViewModel() {

    private val transientError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        observeBackgroundProtectionEnabled(),
        observeScanInterval(),
        observeLastScheduledAt(),
        transientError,
    ) { enabled, intervalHours, lastScheduledAt, error ->
        // Cast to the sealed supertype explicitly — same reason as every
        // other stateIn-backed ViewModel in this project (ADR 0030):
        // without it, this lambda's inferred return type is Loaded
        // specifically, and the .catch{} below (emitting a sibling
        // Error) would not type-check against a Flow<Loaded>.
        SettingsUiState.Loaded(
            backgroundProtectionEnabled = enabled,
            selectedInterval = ScanInterval.fromHours(intervalHours),
            lastScheduledAtEpochMillis = lastScheduledAt,
            errorMessage = error,
        ) as SettingsUiState
    }
        .catch { error ->
            emit(SettingsUiState.Error(error.message ?: "Something went wrong loading your settings."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState.Loading,
        )

    fun onBackgroundProtectionToggled(enabled: Boolean) {
        viewModelScope.launch {
            transientError.value = null
            if (enabled) {
                val intervalHours = currentIntervalHoursOrDefault()
                when (val result = scheduleBackgroundScan(intervalHours)) {
                    is AppResult.Success -> recordEnabled(
                        RecordBackgroundProtectionEnabledParams(intervalHours, System.currentTimeMillis()),
                    )
                    is AppResult.Failure -> transientError.value =
                        "Couldn't turn on background protection. Please try again."
                    AppResult.Loading -> Unit
                }
            } else {
                when (cancelBackgroundScan()) {
                    is AppResult.Success -> recordDisabled()
                    is AppResult.Failure -> transientError.value =
                        "Couldn't turn off background protection. Please try again."
                    AppResult.Loading -> Unit
                }
            }
        }
    }

    fun onIntervalSelected(interval: ScanInterval) {
        viewModelScope.launch {
            transientError.value = null
            setScanInterval(interval.hours)

            // Only re-schedule if protection is currently on — selecting
            // an interval while disabled just persists the choice for
            // next time, with nothing running yet to update.
            val currentlyEnabled = (uiState.value as? SettingsUiState.Loaded)?.backgroundProtectionEnabled == true
            if (currentlyEnabled) {
                when (val result = scheduleBackgroundScan(interval.hours)) {
                    is AppResult.Success -> recordEnabled(
                        RecordBackgroundProtectionEnabledParams(interval.hours, System.currentTimeMillis()),
                    )
                    is AppResult.Failure -> transientError.value =
                        "Couldn't update the scan interval. Please try again."
                    AppResult.Loading -> Unit
                }
            }
        }
    }

    fun dismissError() {
        transientError.value = null
    }

    private fun currentIntervalHoursOrDefault(): Long =
        (uiState.value as? SettingsUiState.Loaded)?.selectedInterval?.hours ?: ScanInterval.DAILY.hours

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Loaded(
        val backgroundProtectionEnabled: Boolean,
        val selectedInterval: ScanInterval,
        val lastScheduledAtEpochMillis: Long?,
        val errorMessage: String? = null,
    ) : SettingsUiState

    data class Error(val message: String) : SettingsUiState
}

/** Local to feature:settings, same rule-of-three reasoning as ADR 0032/
 *  ADR 0034 — a small, presentation-facing choice set, not shared. */
enum class ScanInterval(val hours: Long, val label: String) {
    DAILY(24L, "Daily"),
    EVERY_3_DAYS(72L, "Every 3 Days"),
    WEEKLY(168L, "Weekly"),
    ;

    companion object {
        fun fromHours(hours: Long): ScanInterval = entries.find { it.hours == hours } ?: DAILY
    }
}
