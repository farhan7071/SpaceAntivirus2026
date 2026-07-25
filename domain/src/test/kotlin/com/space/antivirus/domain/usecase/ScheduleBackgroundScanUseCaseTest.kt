package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.domain.repository.BackgroundScanScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Sprint 025: ScheduleBackgroundScanUseCase now takes intervalHours as an
 * explicit UseCase<Long, Unit> parameter rather than NoParamsUseCase's
 * implicit no-args shape — every call site here passes an explicit value.
 */
class ScheduleBackgroundScanUseCaseTest {

    private val scheduler = mockk<BackgroundScanScheduler>()

    @Test
    fun `delegates directly to the scheduler with the given interval and returns its result`() = runTest {
        coEvery { scheduler.schedulePeriodicScan(24L) } returns AppResult.Success(Unit)
        val useCase = ScheduleBackgroundScanUseCase(scheduler, StandardTestDispatcher(testScheduler))

        val result = useCase(24L)

        assertThat(result).isEqualTo(AppResult.Success(Unit))
        coVerify(exactly = 1) { scheduler.schedulePeriodicScan(24L) }
    }

    @Test
    fun `passes through whatever interval the caller provides, not a hardcoded value`() = runTest {
        coEvery { scheduler.schedulePeriodicScan(6L) } returns AppResult.Success(Unit)
        val useCase = ScheduleBackgroundScanUseCase(scheduler, StandardTestDispatcher(testScheduler))

        useCase(6L)

        coVerify(exactly = 1) { scheduler.schedulePeriodicScan(6L) }
        coVerify(exactly = 0) { scheduler.schedulePeriodicScan(24L) }
    }

    @Test
    fun `propagates a scheduler failure unchanged`() = runTest {
        coEvery { scheduler.schedulePeriodicScan(24L) } returns
            AppResult.Failure(AppError.Unexpected(RuntimeException("boom")))
        val useCase = ScheduleBackgroundScanUseCase(scheduler, StandardTestDispatcher(testScheduler))

        val result = useCase(24L)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }

    @Test
    fun `propagates an invalid-interval rejection from the scheduler unchanged`() = runTest {
        coEvery { scheduler.schedulePeriodicScan(0L) } returns
            AppResult.Failure(AppError.InvalidScheduleConfiguration("intervalHours must be at least 1, got 0"))
        val useCase = ScheduleBackgroundScanUseCase(scheduler, StandardTestDispatcher(testScheduler))

        val result = useCase(0L)

        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.InvalidScheduleConfiguration::class.java)
    }
}
