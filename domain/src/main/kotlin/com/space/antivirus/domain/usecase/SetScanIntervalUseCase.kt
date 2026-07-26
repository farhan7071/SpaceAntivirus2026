package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.BackgroundProtectionPreferences
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/** Persists an interval choice on its own — used when background
 *  protection is currently disabled, so there's nothing to re-schedule
 *  yet; the SettingsViewModel calls ScheduleBackgroundScanUseCase
 *  separately when protection is already enabled and the interval
 *  changes, since that path needs to know the outcome to decide whether
 *  to also call RecordBackgroundProtectionEnabledUseCase again. */
class SetScanIntervalUseCase @Inject constructor(
    private val preferences: BackgroundProtectionPreferences,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<Long, Unit>(dispatcher) {

    override suspend fun execute(params: Long): AppResult<Unit> {
        preferences.setIntervalHours(params)
        return AppResult.Success(Unit)
    }
}
