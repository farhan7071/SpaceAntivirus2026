package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.domain.NoParamsUseCase
import com.space.antivirus.domain.repository.BackgroundScanScheduler
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

class CancelBackgroundScanUseCase @Inject constructor(
    private val scheduler: BackgroundScanScheduler,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : NoParamsUseCase<Unit>(dispatcher) {

    override suspend fun execute(): AppResult<Unit> = scheduler.cancelScheduledScan()
}
