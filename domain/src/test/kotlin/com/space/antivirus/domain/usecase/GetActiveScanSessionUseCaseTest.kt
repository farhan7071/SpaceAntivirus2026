package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.domain.fake.FakeSecurityRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetActiveScanSessionUseCaseTest {

    @Test
    fun `returns null when nothing is running`() = runTest {
        val repository = FakeSecurityRepository()
        val useCase = GetActiveScanSessionUseCase(repository, StandardTestDispatcher(testScheduler))

        val result = useCase()

        assertThat(result).isEqualTo(AppResult.Success(null))
    }

    @Test
    fun `returns the session once one has been started`() = runTest {
        val repository = FakeSecurityRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val started = (StartScanSessionUseCase(repository, dispatcher)(ScanType.QUICK) as AppResult.Success).data
        val useCase = GetActiveScanSessionUseCase(repository, dispatcher)

        val result = useCase()

        assertThat(result).isEqualTo(AppResult.Success(started))
    }

    @Test
    fun `returns null again once the session completes`() = runTest {
        val repository = FakeSecurityRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val session = (StartScanSessionUseCase(repository, dispatcher)(ScanType.QUICK) as AppResult.Success).data
        CompleteScanSessionUseCase(repository, dispatcher)(
            CompleteScanSessionParams(
                sessionId = session.id,
                statistics = ScanStatistics(
                    itemsScanned = 1,
                    threatsFound = 0,
                    itemsInconclusive = 0,
                    durationMillis = 10,
                ),
                threats = emptyList(),
            ),
        )
        val useCase = GetActiveScanSessionUseCase(repository, dispatcher)

        val result = useCase()

        assertThat(result).isEqualTo(AppResult.Success(null))
    }
}
