package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.TrustedItemType
import com.space.antivirus.domain.fake.FakeTrustedItemRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class IsTrustedUseCaseTest {

    @Test
    fun `returns false for an identifier never added`() = runTest {
        val repository = FakeTrustedItemRepository()
        val useCase = IsTrustedUseCase(repository, StandardTestDispatcher(testScheduler))

        val result = useCase("com.example.unknown")

        assertThat(result).isEqualTo(AppResult.Success(false))
    }

    @Test
    fun `returns true once an identifier has been trusted`() = runTest {
        val repository = FakeTrustedItemRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        AddTrustedItemUseCase(repository, dispatcher)(
            AddTrustedItemParams("com.example.knownapp", TrustedItemType.APPLICATION),
        )

        val result = IsTrustedUseCase(repository, dispatcher)("com.example.knownapp")

        assertThat(result).isEqualTo(AppResult.Success(true))
    }
}
