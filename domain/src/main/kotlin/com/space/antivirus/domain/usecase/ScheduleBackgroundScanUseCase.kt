package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.domain.UseCase
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
 *
 * Sprint 025: takes intervalHours as an explicit parameter rather than
 * NoParamsUseCase's implicit no-args shape — matches every other
 * parameterized UseCase in this project (UseCase<Params, Result>, not a
 * default value snuck in at the override site, since Kotlin doesn't
 * allow introducing a default in an overriding method the abstract base
 * didn't declare). Callers wanting the standing default explicitly pass
 * BackgroundScanScheduler.DEFAULT_INTERVAL_HOURS.
 */
class ScheduleBackgroundScanUseCase @Inject constructor(
    private val scheduler: BackgroundScanScheduler,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<Long, Unit>(dispatcher) {

    override suspend fun execute(params: Long): AppResult<Unit> = scheduler.schedulePeriodicScan(params)
}
