package com.space.antivirus.core.trusteddata

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.database.AppDatabase
import com.space.antivirus.core.model.TrustedItemType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented — same reasoning as SecurityRepositoryImplTest (Sprint
 * 011) and every DAO test since Sprint 010: no Robolectric, needs a real
 * SQLite environment. Runs during physical-device verification.
 */
@RunWith(AndroidJUnit4::class)
class TrustedItemRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TrustedItemRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = TrustedItemRepositoryImpl(database.trustedItemDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addTrustedItem_createsANewItem() = runTest {
        val result = repository.addTrustedItem("com.example.app", TrustedItemType.APPLICATION)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val item = (result as AppResult.Success).data
        assertThat(item.identifier).isEqualTo("com.example.app")
        assertThat(item.type).isEqualTo(TrustedItemType.APPLICATION)
    }

    @Test
    fun addTrustedItem_isIdempotentByIdentifierAndType_returnsTheExistingItem() = runTest {
        val first = (
            repository.addTrustedItem("com.example.app", TrustedItemType.APPLICATION) as AppResult.Success
            ).data

        val second = (
            repository.addTrustedItem("com.example.app", TrustedItemType.APPLICATION) as AppResult.Success
            ).data

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun addTrustedItem_sameIdentifierDifferentType_createsADistinctItem() = runTest {
        val fileItem = (
            repository.addTrustedItem("shared-name", TrustedItemType.FILE) as AppResult.Success
            ).data
        val appItem = (
            repository.addTrustedItem("shared-name", TrustedItemType.APPLICATION) as AppResult.Success
            ).data

        assertThat(fileItem.id).isNotEqualTo(appItem.id)
    }

    @Test
    fun removeTrustedItem_removesAnExistingItem() = runTest {
        val item = (
            repository.addTrustedItem("com.example.app", TrustedItemType.APPLICATION) as AppResult.Success
            ).data

        val result = repository.removeTrustedItem(item.id)

        assertThat(result).isEqualTo(AppResult.Success(Unit))
        assertThat(repository.isTrusted("com.example.app")).isEqualTo(AppResult.Success(false))
    }

    @Test
    fun removeTrustedItem_failsWithNotFound_forAnUnknownId() = runTest {
        val result = repository.removeTrustedItem("does-not-exist")

        assertThat(result).isEqualTo(AppResult.Failure(AppError.TrustedItemNotFound("does-not-exist")))
    }

    @Test
    fun isTrusted_isFalse_forAnIdentifierNeverAdded() = runTest {
        assertThat(repository.isTrusted("com.example.unknown")).isEqualTo(AppResult.Success(false))
    }

    @Test
    fun isTrusted_isTrue_onceAdded() = runTest {
        repository.addTrustedItem("com.example.app", TrustedItemType.APPLICATION)

        assertThat(repository.isTrusted("com.example.app")).isEqualTo(AppResult.Success(true))
    }

    @Test
    fun observeTrustedItems_emitsAnUpdatedList_whenAnItemIsAdded() = runTest {
        repository.observeTrustedItems().test {
            assertThat(awaitItem()).isEmpty()

            repository.addTrustedItem("com.example.app", TrustedItemType.APPLICATION)

            val updated = awaitItem()
            assertThat(updated).hasSize(1)
            assertThat(updated.first().identifier).isEqualTo("com.example.app")
        }
    }
}
