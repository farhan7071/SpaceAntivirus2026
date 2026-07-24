package com.space.antivirus.core.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.database.AppDatabase
import com.space.antivirus.core.database.entity.ScanSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented (not JVM unit) test — needs a real SQLite environment this
 * sandbox doesn't have. Runs during physical-device verification. See
 * core:database's build.gradle.kts and ADR 0023.
 */
@RunWith(AndroidJUnit4::class)
class ScanSessionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ScanSessionDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()
        dao = database.scanSessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun session(
        id: String,
        state: String,
        completedAt: Long? = null,
    ) = ScanSessionEntity(
        id = id,
        scanType = "QUICK",
        state = state,
        startedAtEpochMillis = 0L,
        completedAtEpochMillis = completedAt,
    )

    @Test
    fun upsertThenGetById_returnsTheSameSession() = runTest {
        dao.upsert(session("s1", "PENDING"))

        val result = dao.getById("s1")

        assertThat(result?.state).isEqualTo("PENDING")
    }

    @Test
    fun upsert_withSameId_replacesRatherThanDuplicating() = runTest {
        dao.upsert(session("s1", "PENDING"))
        dao.upsert(session("s1", "RUNNING"))

        val result = dao.getById("s1")

        assertThat(result?.state).isEqualTo("RUNNING")
    }

    @Test
    fun getActive_returnsAPendingOrRunningSession_notCompleted() = runTest {
        dao.upsert(session("s1", "COMPLETED", completedAt = 100L))
        dao.upsert(session("s2", "RUNNING"))

        val active = dao.getActive()

        assertThat(active?.id).isEqualTo("s2")
    }

    @Test
    fun getActive_returnsNull_whenNothingIsRunning() = runTest {
        dao.upsert(session("s1", "COMPLETED", completedAt = 100L))

        assertThat(dao.getActive()).isNull()
    }

    @Test
    fun observeCompleted_emitsOnlyCompletedSessions_mostRecentFirst() = runTest {
        dao.upsert(session("s1", "COMPLETED", completedAt = 100L))
        dao.upsert(session("s2", "RUNNING"))
        dao.upsert(session("s3", "COMPLETED", completedAt = 200L))

        val completed = dao.observeCompleted().first()

        assertThat(completed.map { it.id }).containsExactly("s3", "s1").inOrder()
    }

    @Test
    fun getMostRecentCompleted_returnsTheLatestOne() = runTest {
        dao.upsert(session("s1", "COMPLETED", completedAt = 100L))
        dao.upsert(session("s2", "COMPLETED", completedAt = 200L))

        assertThat(dao.getMostRecentCompleted()?.id).isEqualTo("s2")
    }

    @Test
    fun deleteById_removesOnlyThatSession() = runTest {
        dao.upsert(session("s1", "COMPLETED", completedAt = 100L))
        dao.upsert(session("s2", "COMPLETED", completedAt = 200L))

        dao.deleteById("s1")

        assertThat(dao.getById("s1")).isNull()
        assertThat(dao.getById("s2")).isNotNull()
    }

    @Test
    fun clearAll_removesEverySession() = runTest {
        dao.upsert(session("s1", "COMPLETED", completedAt = 100L))
        dao.upsert(session("s2", "RUNNING"))

        dao.clearAll()

        assertThat(dao.getById("s1")).isNull()
        assertThat(dao.getActive()).isNull()
    }
}
