package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.domain.NoParamsUseCase
import com.space.antivirus.domain.repository.BackgroundScanScheduler
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/**
 * This sprint's foundation, deliberately not called from anywhere yet —
 * no Settings toggle exists to let a user opt in/out of automatic
 * background scanning, and silently auto-enabling it for every install
 * without that decision would be a real product/consent choice this
 * sprint has no business making unilaterally. This UseCase exists, is
 * fully real and tested, and is exactly what a future Settings toggle
 * will call — building the capability now without deciding when it
 * activates is the deliberate scope boundary for "the foundation," per
 * this sprint's own framing.
 */
class ScheduleBackgroundScanUseCase @Inject constructor(
    private val scheduler: BackgroundScanScheduler,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : NoParamsUseCase<Unit>(dispatcher) {

    override suspend fun execute(): AppResult<Unit> = scheduler.schedulePeriodicScan()
}
