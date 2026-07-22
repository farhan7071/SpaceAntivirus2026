package com.space.antivirus.domain

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

private class EchoUseCase(dispatcher: kotlinx.coroutines.CoroutineDispatcher) :
    UseCase<Int, Int>(dispatcher) {
    override suspend fun execute(params: Int): AppResult<Int> = AppResult.Success(params)
}

class UseCaseTest {

    @Test
    fun `invoke returns wrapped success`() = runTest {
        val useCase = EchoUseCase(StandardTestDispatcher(testScheduler))
        val result = useCase(5)
        assertThat(result).isEqualTo(AppResult.Success(5))
    }
}
