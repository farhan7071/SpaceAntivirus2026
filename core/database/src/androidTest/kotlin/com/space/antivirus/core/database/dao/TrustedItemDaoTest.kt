package com.space.antivirus.core.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.database.AppDatabase
import com.space.antivirus.core.database.entity.TrustedItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrustedItemDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TrustedItemDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.trustedItemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun item(id: String, identifier: String, type: String, addedAt: Long = 0L) = TrustedItemEntity(
        id = id,
        identifier = identifier,
        type = type,
        addedAtEpochMillis = addedAt,
        reason = null,
    )

    @Test
    fun insertThenFindByIdentifierAndType_returnsTheItem() = runTest {
        dao.insert(item("t1", "com.example.app", "APPLICATION"))

        val result = dao.findByIdentifierAndType("com.example.app", "APPLICATION")

        assertThat(result?.id).isEqualTo("t1")
    }

    @Test
    fun findByIdentifierAndType_returnsNull_whenTypeDiffers() = runTest {
        dao.insert(item("t1", "shared-name", "FILE"))

        val result = dao.findByIdentifierAndType("shared-name", "APPLICATION")

        assertThat(result).isNull()
    }

    @Test
    fun existsByIdentifier_isTrue_regardlessOfType() = runTest {
        dao.insert(item("t1", "com.example.app", "APPLICATION"))

        assertThat(dao.existsByIdentifier("com.example.app")).isTrue()
    }

    @Test
    fun existsByIdentifier_isFalse_whenNeverTrusted() = runTest {
        assertThat(dao.existsByIdentifier("com.example.unknown")).isFalse()
    }

    @Test
    fun deleteById_returnsOne_andRemovesTheRow() = runTest {
        dao.insert(item("t1", "com.example.app", "APPLICATION"))

        val deletedCount = dao.deleteById("t1")

        assertThat(deletedCount).isEqualTo(1)
        assertThat(dao.existsByIdentifier("com.example.app")).isFalse()
    }

    @Test
    fun deleteById_returnsZero_forAnUnknownId() = runTest {
        val deletedCount = dao.deleteById("does-not-exist")

        assertThat(deletedCount).isEqualTo(0)
    }

    @Test
    fun observeAll_emitsMostRecentlyAddedFirst() = runTest {
        dao.insert(item("t1", "first", "FILE", addedAt = 100L))
        dao.insert(item("t2", "second", "FILE", addedAt = 200L))

        val all = dao.observeAll().first()

        assertThat(all.map { it.id }).containsExactly("t2", "t1").inOrder()
    }
}
