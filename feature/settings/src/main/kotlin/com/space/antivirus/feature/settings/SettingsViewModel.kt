package com.space.antivirus.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AppInfo
import com.space.antivirus.domain.usecase.GetAppInfoUseCase
import com.space.antivirus.domain.usecase.GetBatteryOptimizationStatusUseCase
import com.space.antivirus.domain.usecase.ObserveProtectionStateUseCase
import com.space.antivirus.domain.usecase.SetNotifyAfterScanUseCase
import com.space.antivirus.domain.usecase.SetProtectionEnabledUseCase
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
 * Replaces the Sprint 003 placeholder.
 *
 * Sprint 042: routes everything through ProtectionManager rather than
 * calling the scheduler and preferences use cases itself. Those six use
 * cases (ScheduleBackgroundScan, CancelBackgroundScan, the two Record*
 * and the two Observe*) were deleted in the same sprint — once Home's
 * quick toggle and the boot receiver also needed this behaviour, having
 * three callers each re-deriving "schedule, then persist only on
 * confirmed success, then update the notification" is exactly how they
 * drift. The ordering lives in one place now.
 *
 * Reactive Flow-combine shape (like HomeViewModel/SecurityCenterViewModel),
 * not action-triggered like ScanViewModel/CleanViewModel — the underlying
 * data here (persisted preferences) IS genuinely ongoing, observable
 * state, unlike a one-shot scan or file enumeration.
 *
 * PERSIST-ONLY-ON-CONFIRMED-SUCCESS still holds — it just isn't this
 * class's job to enforce any more. See ProtectionManagerImpl, where the
 * ordering is now structural rather than repeated per caller.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeProtectionState: ObserveProtectionStateUseCase,
    private val setProtectionEnabled: SetProtectionEnabledUseCase,
    private val setNotifyAfterScan: SetNotifyAfterScanUseCase,
    private val setScanInterval: SetScanIntervalUseCase,
    private val getBatteryOptimizationStatus: GetBatteryOptimizationStatusUseCase,
    private val getAppInfo: GetAppInfoUseCase,
) : ViewModel() {

    private val transientError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        observeProtectionState(),
        transientError,
    ) { protection, error ->
        // Cast to the sealed supertype explicitly — same reason as every
        // other stateIn-backed ViewModel in this project (ADR 0030):
        // without it, this lambda's inferred return type is Loaded
        // specifically, and the .catch{} below (emitting a sibling
        // Error) would not type-check against a Flow<Loaded>.
        SettingsUiState.Loaded(
            backgroundProtectionEnabled = protection.isEnabled,
            selectedInterval = ScanInterval.fromHours(protection.intervalHours),
            lastScheduledAtEpochMillis = protection.lastScheduledAtEpochMillis,
            notifyAfterScan = protection.notifyAfterScan,
            // Read once per state emission rather than observed: this is
            // a system setting the user changes outside this app, and
            // there is no broadcast for it. A stale value here is
            // harmless — the card is informational and the underlying
            // setting is one tap away in system settings.
            isIgnoringBatteryOptimizations = getBatteryOptimizationStatus(),
            appInfo = getAppInfo(),
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
            // Sprint 042: the schedule-then-persist-then-notify ordering
            // this method used to spell out now lives in
            // ProtectionManager, so Home's quick toggle and this switch
            // cannot drift apart.
            val result = setProtectionEnabled(
                enabled = enabled,
                intervalHours = if (enabled) currentIntervalHoursOrDefault() else null,
            )
            if (result is AppResult.Failure) {
                transientError.value = if (enabled) {
                    "Couldn't turn on background protection. Please try again."
                } else {
                    "Couldn't turn off background protection. Please try again."
                }
            }
        }
    }

    fun onNotifyAfterScanToggled(enabled: Boolean) {
        viewModelScope.launch { setNotifyAfterScan(enabled) }
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
                // Re-enabling at the new interval replaces the existing
                // unique work rather than adding a second schedule.
                val result = setProtectionEnabled(enabled = true, intervalHours = interval.hours)
                if (result is AppResult.Failure) {
                    transientError.value = "Couldn't update the scan interval. Please try again."
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
        val notifyAfterScan: Boolean = false,
        val isIgnoringBatteryOptimizations: Boolean = true,
        val appInfo: AppInfo? = null,
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
