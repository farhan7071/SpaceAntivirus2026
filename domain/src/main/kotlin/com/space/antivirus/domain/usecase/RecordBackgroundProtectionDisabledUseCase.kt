package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.domain.NoParamsUseCase
import com.space.antivirus.domain.repository.BackgroundProtectionPreferences
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

class RecordBackgroundProtectionDisabledUseCase @Inject constructor(
    private val preferences: BackgroundProtectionPreferences,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : NoParamsUseCase<Unit>(dispatcher) {

    override suspend fun execute(): AppResult<Unit> {
        preferences.recordDisabled()
        return AppResult.Success(Unit)
    }
}
