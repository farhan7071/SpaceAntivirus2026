package com.space.antivirus.core.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.database.AppDatabase
import com.space.antivirus.core.database.entity.ScanSessionEntity
import com.space.antivirus.core.database.entity.ScanStatisticsEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanStatisticsDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var statisticsDao: ScanStatisticsDao
    private lateinit var sessionDao: ScanSessionDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        statisticsDao = database.scanStatisticsDao()
        sessionDao = database.scanSessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertCompletedSession(id: String) {
        sessionDao.upsert(
            ScanSessionEntity(
                id = id,
                scanType = "QUICK",
                state = "COMPLETED",
                startedAtEpochMillis = 0L,
                completedAtEpochMillis = 100L,
            ),
        )
    }

    @Test
    fun upsertThenGetBySessionId_returnsTheSameStatistics() = runTest {
        insertCompletedSession("s1")
        statisticsDao.upsert(
            ScanStatisticsEntity(
                sessionId = "s1",
                itemsScanned = 10,
                threatsFound = 1,
                itemsInconclusive = 0,
                itemsTrusted = 2,
                durationMillis = 500,
            ),
        )

        val result = statisticsDao.getBySessionId("s1")

        assertThat(result?.itemsScanned).isEqualTo(10)
        assertThat(result?.itemsTrusted).isEqualTo(2)
    }

    @Test
    fun getBySessionId_returnsNull_whenNoStatisticsExistYet() = runTest {
        insertCompletedSession("s1")

        assertThat(statisticsDao.getBySessionId("s1")).isNull()
    }

    @Test
    fun deletingTheSession_cascadesToDeleteItsStatistics() = runTest {
        insertCompletedSession("s1")
        statisticsDao.upsert(
            ScanStatisticsEntity(
                sessionId = "s1",
                itemsScanned = 10,
                threatsFound = 0,
                itemsInconclusive = 0,
                itemsTrusted = 0,
                durationMillis = 500,
            ),
        )

        sessionDao.deleteById("s1")

        assertThat(statisticsDao.getBySessionId("s1")).isNull()
    }
}
