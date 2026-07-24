package com.space.antivirus.core.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.database.AppDatabase
import com.space.antivirus.core.database.entity.DetectionEntity
import com.space.antivirus.core.database.entity.ScanSessionEntity
import com.space.antivirus.core.database.entity.ThreatEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetectionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var detectionDao: DetectionDao
    private lateinit var threatDao: ThreatDao
    private lateinit var sessionDao: ScanSessionDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        detectionDao = database.detectionDao()
        threatDao = database.threatDao()
        sessionDao = database.scanSessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertThreat(threatId: String, sessionId: String) {
        sessionDao.upsert(
            ScanSessionEntity(
                id = sessionId,
                scanType = "FULL",
                state = "COMPLETED",
                startedAtEpochMillis = 0L,
                completedAtEpochMillis = 100L,
            ),
        )
        threatDao.insertAll(
            listOf(
                ThreatEntity(
                    id = threatId,
                    sessionId = sessionId,
                    targetIdentifier = "com.example.app",
                    threatType = "MALWARE",
                    riskLevel = "ACTION_NEEDED",
                    title = "Test threat",
                    description = "Test description",
                    discoveredAtEpochMillis = 0L,
                ),
            ),
        )
    }

    private fun detection(id: String, threatId: String) = DetectionEntity(
        id = id,
        threatId = threatId,
        analyzerId = "test-analyzer",
        threatType = "MALWARE",
        evidenceDescription = "matched a known-bad signature",
        riskLevel = "ACTION_NEEDED",
    )

    @Test
    fun insertAllThenGetByThreatId_returnsAllDetectionsForThatThreat() = runTest {
        insertThreat("t1", "s1")
        detectionDao.insertAll(listOf(detection("d1", "t1"), detection("d2", "t1")))

        val result = detectionDao.getByThreatId("t1")

        assertThat(result.map { it.id }).containsExactly("d1", "d2")
    }

    @Test
    fun deletingTheThreat_cascadesToDeleteItsDetections() = runTest {
        insertThreat("t1", "s1")
        detectionDao.insertAll(listOf(detection("d1", "t1")))

        // Deleting the session cascades to the threat (ThreatEntity's own
        // foreign key), which should in turn cascade to its detections —
        // verifying the whole chain, not just one hop.
        sessionDao.deleteById("s1")

        assertThat(detectionDao.getByThreatId("t1")).isEmpty()
    }
}
