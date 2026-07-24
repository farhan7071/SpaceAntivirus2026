package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.TrustedItemType
import com.space.antivirus.domain.fake.FakeTrustedItemRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddTrustedItemUseCaseTest {

    @Test
    fun `adds a new trusted item`() = runTest {
        val repository = FakeTrustedItemRepository()
        val useCase = AddTrustedItemUseCase(repository, StandardTestDispatcher(testScheduler))

        val result = useCase(AddTrustedItemParams("com.example.knownapp", TrustedItemType.APPLICATION))

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val item = (result as AppResult.Success).data
        assertThat(item.identifier).isEqualTo("com.example.knownapp")
        assertThat(item.type).isEqualTo(TrustedItemType.APPLICATION)
    }

    @Test
    fun `adding the same identifier and type twice returns the existing item, not a duplicate`() = runTest {
        val repository = FakeTrustedItemRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val useCase = AddTrustedItemUseCase(repository, dispatcher)
        val params = AddTrustedItemParams("com.example.knownapp", TrustedItemType.APPLICATION)

        val first = (useCase(params) as AppResult.Success).data
        val second = (useCase(params) as AppResult.Success).data

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `same identifier under a different type is a separate trusted item`() = runTest {
        val repository = FakeTrustedItemRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val useCase = AddTrustedItemUseCase(repository, dispatcher)

        val fileItem = (useCase(AddTrustedItemParams("shared-name", TrustedItemType.FILE)) as AppResult.Success).data
        val appItem = (useCase(AddTrustedItemParams("shared-name", TrustedItemType.APPLICATION)) as AppResult.Success).data

        assertThat(fileItem.id).isNotEqualTo(appItem.id)
    }

    @Test
    fun `propagates a repository failure`() = runTest {
        val repository = FakeTrustedItemRepository().apply { forcedFailure = AppError.StorageUnavailable }
        val useCase = AddTrustedItemUseCase(repository, StandardTestDispatcher(testScheduler))

        val result = useCase(AddTrustedItemParams("com.example.app", TrustedItemType.APPLICATION))

        assertThat(result).isEqualTo(AppResult.Failure(AppError.StorageUnavailable))
    }
}
