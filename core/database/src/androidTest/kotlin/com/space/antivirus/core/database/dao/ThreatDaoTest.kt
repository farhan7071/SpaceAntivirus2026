package com.space.antivirus.core.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.database.AppDatabase
import com.space.antivirus.core.database.entity.ScanSessionEntity
import com.space.antivirus.core.database.entity.ThreatEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThreatDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var threatDao: ThreatDao
    private lateinit var sessionDao: ScanSessionDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        threatDao = database.threatDao()
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
                scanType = "FULL",
                state = "COMPLETED",
                startedAtEpochMillis = 0L,
                completedAtEpochMillis = 100L,
            ),
        )
    }

    private fun threat(id: String, sessionId: String) = ThreatEntity(
        id = id,
        sessionId = sessionId,
        targetIdentifier = "com.example.app",
        threatType = "MALWARE",
        riskLevel = "ACTION_NEEDED",
        title = "Test threat",
        description = "Test description",
        discoveredAtEpochMillis = 0L,
    )

    @Test
    fun insertAllThenGetBySessionId_returnsAllThreatsForThatSession() = runTest {
        insertCompletedSession("s1")
        threatDao.insertAll(listOf(threat("t1", "s1"), threat("t2", "s1")))

        val result = threatDao.getBySessionId("s1")

        assertThat(result.map { it.id }).containsExactly("t1", "t2")
    }

    @Test
    fun getBySessionId_doesNotReturnThreatsFromOtherSessions() = runTest {
        insertCompletedSession("s1")
        insertCompletedSession("s2")
        threatDao.insertAll(listOf(threat("t1", "s1"), threat("t2", "s2")))

        val result = threatDao.getBySessionId("s1")

        assertThat(result.map { it.id }).containsExactly("t1")
    }

    @Test
    fun deletingTheSession_cascadesToDeleteItsThreats() = runTest {
        insertCompletedSession("s1")
        threatDao.insertAll(listOf(threat("t1", "s1")))

        sessionDao.deleteById("s1")

        assertThat(threatDao.getBySessionId("s1")).isEmpty()
    }
}
