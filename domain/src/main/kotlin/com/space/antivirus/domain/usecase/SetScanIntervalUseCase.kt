package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.BackgroundProtectionPreferences
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/** Persists an interval choice on its own — used when background
 *  protection is currently disabled, so there's nothing to re-schedule
 *  yet. When protection is already enabled, SettingsViewModel follows
 *  this with SetProtectionEnabledUseCase, which re-schedules at the new
 *  interval and re-persists together (Sprint 042). */
class SetScanIntervalUseCase @Inject constructor(
    private val preferences: BackgroundProtectionPreferences,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<Long, Unit>(dispatcher) {

    override suspend fun execute(params: Long): AppResult<Unit> {
        preferences.setIntervalHours(params)
        return AppResult.Success(Unit)
    }
}
