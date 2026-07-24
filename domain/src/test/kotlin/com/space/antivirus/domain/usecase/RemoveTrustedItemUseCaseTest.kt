package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.TrustedItemType
import com.space.antivirus.domain.fake.FakeTrustedItemRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoveTrustedItemUseCaseTest {

    @Test
    fun `removes an existing trusted item`() = runTest {
        val repository = FakeTrustedItemRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val added = (
            AddTrustedItemUseCase(repository, dispatcher)(
                AddTrustedItemParams("com.example.app", TrustedItemType.APPLICATION),
            ) as AppResult.Success
            ).data

        val result = RemoveTrustedItemUseCase(repository, dispatcher)(added.id)

        assertThat(result).isEqualTo(AppResult.Success(Unit))
    }

    @Test
    fun `removing a non-existent id fails with TrustedItemNotFound`() = runTest {
        val repository = FakeTrustedItemRepository()
        val useCase = RemoveTrustedItemUseCase(repository, StandardTestDispatcher(testScheduler))

        val result = useCase("does-not-exist")

        assertThat(result).isEqualTo(AppResult.Failure(AppError.TrustedItemNotFound("does-not-exist")))
    }
}
