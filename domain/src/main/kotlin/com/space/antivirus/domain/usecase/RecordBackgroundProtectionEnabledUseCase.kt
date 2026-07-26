package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.BackgroundProtectionPreferences
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/** Deliberately takes RecordBackgroundProtectionEnabledParams, not just
 *  intervalHours — the caller (SettingsViewModel) is expected to call
 *  this only AFTER ScheduleBackgroundScanUseCase has already succeeded,
 *  never speculatively; passing the confirmed interval and a real
 *  timestamp together is what keeps lastScheduledAtEpochMillis an honest
 *  signal rather than an assumption (see BackgroundProtectionPreferences'
 *  own KDoc). */
class RecordBackgroundProtectionEnabledUseCase @Inject constructor(
    private val preferences: BackgroundProtectionPreferences,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<RecordBackgroundProtectionEnabledParams, Unit>(dispatcher) {

    override suspend fun execute(params: RecordBackgroundProtectionEnabledParams): AppResult<Unit> {
        preferences.recordEnabled(params.intervalHours, params.scheduledAtEpochMillis)
        return AppResult.Success(Unit)
    }
}

data class RecordBackgroundProtectionEnabledParams(
    val intervalHours: Long,
    val scheduledAtEpochMillis: Long,
)
