package com.space.antivirus.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.TrustedItemType
import com.space.antivirus.domain.fake.FakeTrustedItemRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveTrustedItemsUseCaseTest {

    @Test
    fun `emits an updated list when an item is added`() = runTest {
        val repository = FakeTrustedItemRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val observeTrustedItems = ObserveTrustedItemsUseCase(repository)

        observeTrustedItems().test {
            assertThat(awaitItem()).isEmpty()

            AddTrustedItemUseCase(repository, dispatcher)(
                AddTrustedItemParams("com.example.app", TrustedItemType.APPLICATION),
            )

            val updated = awaitItem()
            assertThat(updated).hasSize(1)
            assertThat(updated.first().identifier).isEqualTo("com.example.app")
        }
    }
}
