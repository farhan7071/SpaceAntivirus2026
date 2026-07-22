package com.space.antivirus.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.domain.fake.FakeSecurityRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveScanHistoryUseCaseTest {

    @Test
    fun `emits a new entry when a scan completes`() = runTest {
        val repository = FakeSecurityRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val observeHistory = ObserveScanHistoryUseCase(repository)

        observeHistory().test {
            assertThat(awaitItem()).isEmpty()

            val session = (
                StartScanSessionUseCase(repository, dispatcher)(ScanType.QUICK) as AppResult.Success
                ).data
            CompleteScanSessionUseCase(repository, dispatcher)(
                CompleteScanSessionParams(
                    sessionId = session.id,
                    statistics = ScanStatistics(itemsScanned = 100, threatsFound = 0, durationMillis = 500),
                    threats = emptyList(),
                ),
            )

            val history = awaitItem()
            assertThat(history).hasSize(1)
            assertThat(history.first().session.id).isEqualTo(session.id)
        }
    }
}
