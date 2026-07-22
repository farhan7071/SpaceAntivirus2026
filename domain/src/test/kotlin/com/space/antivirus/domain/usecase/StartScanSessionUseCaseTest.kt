package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.domain.fake.FakeSecurityRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class StartScanSessionUseCaseTest {

    @Test
    fun `creates and starts a session in one call`() = runTest {
        val repository = FakeSecurityRepository()
        val useCase = StartScanSessionUseCase(repository, StandardTestDispatcher(testScheduler))

        val result = useCase(ScanType.QUICK)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val session = (result as AppResult.Success).data
        assertThat(session.state).isEqualTo(ScanSessionState.RUNNING)
        assertThat(session.scanType).isEqualTo(ScanType.QUICK)
    }

    @Test
    fun `propagates repository failure from the create step without calling start`() = runTest {
        val repository = FakeSecurityRepository().apply { forcedFailure = AppError.StorageUnavailable }
        val useCase = StartScanSessionUseCase(repository, StandardTestDispatcher(testScheduler))

        val result = useCase(ScanType.FULL)

        assertThat(result).isEqualTo(AppResult.Failure(AppError.StorageUnavailable))
    }
}
