package com.space.antivirus.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.ScanProgress
import com.space.antivirus.domain.fake.FakeSecurityRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveScanProgressUseCaseTest {

    @Test
    fun `emits the starting snapshot then subsequent updates`() = runTest {
        val repository = FakeSecurityRepository()
        val observeProgress = ObserveScanProgressUseCase(repository)

        observeProgress("session-1").test {
            assertThat(awaitItem()).isEqualTo(ScanProgress.starting("session-1"))

            repository.updateScanProgress(
                ScanProgress("session-1", itemsProcessed = 3, totalItems = 10, threatsFoundSoFar = 1),
            )

            assertThat(awaitItem().itemsProcessed).isEqualTo(3)
        }
    }
}
