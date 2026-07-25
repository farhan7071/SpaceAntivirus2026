package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.domain.repository.BackgroundScanScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CancelBackgroundScanUseCaseTest {

    private val scheduler = mockk<BackgroundScanScheduler>()

    @Test
    fun `delegates directly to the scheduler and returns its result`() = runTest {
        coEvery { scheduler.cancelScheduledScan() } returns AppResult.Success(Unit)
        val useCase = CancelBackgroundScanUseCase(scheduler, StandardTestDispatcher(testScheduler))

        val result = useCase()

        assertThat(result).isEqualTo(AppResult.Success(Unit))
        coVerify(exactly = 1) { scheduler.cancelScheduledScan() }
    }
}
