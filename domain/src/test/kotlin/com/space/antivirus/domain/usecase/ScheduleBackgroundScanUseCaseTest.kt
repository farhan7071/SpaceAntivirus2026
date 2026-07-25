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

class ScheduleBackgroundScanUseCaseTest {

    private val scheduler = mockk<BackgroundScanScheduler>()

    @Test
    fun `delegates directly to the scheduler and returns its result`() = runTest {
        coEvery { scheduler.schedulePeriodicScan() } returns AppResult.Success(Unit)
        val useCase = ScheduleBackgroundScanUseCase(scheduler, StandardTestDispatcher(testScheduler))

        val result = useCase()

        assertThat(result).isEqualTo(AppResult.Success(Unit))
        coVerify(exactly = 1) { scheduler.schedulePeriodicScan() }
    }

    @Test
    fun `propagates a scheduler failure unchanged`() = runTest {
        coEvery { scheduler.schedulePeriodicScan() } returns AppResult.Failure(AppError.Unexpected(RuntimeException("boom")))
        val useCase = ScheduleBackgroundScanUseCase(scheduler, StandardTestDispatcher(testScheduler))

        val result = useCase()

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }
}
