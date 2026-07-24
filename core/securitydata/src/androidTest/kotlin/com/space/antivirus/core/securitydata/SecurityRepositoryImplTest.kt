package com.space.antivirus.core.securitydata

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.database.AppDatabase
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanProgress
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.core.model.Threat
import com.space.antivirus.core.model.ThreatType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented (not JVM unit) test, same reasoning as core:database's
 * Sprint 010 DAO tests — this project has no Robolectric, and
 * SecurityRepositoryImpl needs a real SQLite environment (in particular,
 * real AppDatabase.withTransaction behavior, which is impractical to mock
 * reliably since it's a Kotlin extension function). Runs during physical-
 * device verification. See ADR 0024.
 */
@RunWith(AndroidJUnit4::class)
class SecurityRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SecurityRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = SecurityRepositoryImpl(
            appDatabase = database,
            scanSessionDao = database.scanSessionDao(),
            scanStatisticsDao = database.scanStatisticsDao(),
            threatDao = database.threatDao(),
            detectionDao = database.detectionDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleStatistics(threatsFound: Int = 0) = ScanStatistics(
        itemsScanned = 10,
        threatsFound = threatsFound,
        itemsInconclusive = 0,
        itemsTrusted = 0,
        durationMillis = 500,
    )

    private fun sampleThreat(id: String) = Threat(
        id = id,
        targetIdentifier = "com.example.app",
        threatType = ThreatType.MALWARE,
        riskLevel = RiskLevel.ACTION_NEEDED,
        title = "Test threat",
        description = "Test description",
        detections = listOf(
            Detection(
                id = "$id-d1",
                analyzerId = AnalyzerId("test-analyzer"),
                threatType = ThreatType.MALWARE,
                evidenceDescription = "matched a known-bad signature",
                riskLevel = RiskLevel.ACTION_NEEDED,
            ),
        ),
    )

    // --- createScanSession / startScanSession ---

    @Test
    fun createScanSession_startsInPendingState() = runTest {
        val result = repository.createScanSession(ScanType.QUICK)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).data.state).isEqualTo(ScanSessionState.PENDING)
    }

    @Test
    fun startScanSession_transitionsPendingToRunning() = runTest {
        val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data

        val result = repository.startScanSession(session.id)

        assertThat((result as AppResult.Success).data.state).isEqualTo(ScanSessionState.RUNNING)
    }

    @Test
    fun startScanSession_failsWithNotFound_forAnUnknownId() = runTest {
        val result = repository.startScanSession("does-not-exist")

        assertThat(result).isEqualTo(AppResult.Failure(AppError.ScanSessionNotFound("does-not-exist")))
    }

    @Test
    fun startScanSession_failsWithInvalidConfiguration_whenAlreadyRunning() = runTest {
        val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data
        repository.startScanSession(session.id)

        val result = repository.startScanSession(session.id)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.InvalidScanConfiguration::class.java)
    }

    // --- completeScanSession ---

    @Test
    fun completeScanSession_persistsStatisticsAndThreatsWithDetections() = runTest {
        val session = (repository.createScanSession(ScanType.FULL) as AppResult.Success).data
        repository.startScanSession(session.id)
        val threat = sampleThreat("t1")

        val result = repository.completeScanSession(session.id, sampleStatistics(threatsFound = 1), listOf(threat))

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val scanResult = (result as AppResult.Success).data
        assertThat(scanResult.session.state).isEqualTo(ScanSessionState.COMPLETED)
        assertThat(scanResult.threats).hasSize(1)
        assertThat(scanResult.threats.first().detections).hasSize(1)
        assertThat(scanResult.threats.first().detections.first().analyzerId).isEqualTo(AnalyzerId("test-analyzer"))
    }

    @Test
    fun completeScanSession_failsWithNotFound_forAnUnknownId() = runTest {
        val result = repository.completeScanSession("does-not-exist", sampleStatistics(), emptyList())

        assertThat(result).isEqualTo(AppResult.Failure(AppError.ScanSessionNotFound("does-not-exist")))
    }

    // --- cancelScanSession / failScanSession ---

    @Test
    fun cancelScanSession_transitionsToCancelled() = runTest {
        val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data

        val result = repository.cancelScanSession(session.id)

        assertThat((result as AppResult.Success).data.state).isEqualTo(ScanSessionState.CANCELLED)
    }

    @Test
    fun failScanSession_transitionsToFailed() = runTest {
        val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data

        val result = repository.failScanSession(session.id, "engine crashed")

        assertThat((result as AppResult.Success).data.state).isEqualTo(ScanSessionState.FAILED)
    }

    // --- getScanSession / getScanResult ---

    @Test
    fun getScanResult_failsWithInvalidConfiguration_forANonTerminalSession() = runTest {
        val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data
        repository.startScanSession(session.id)

        val result = repository.getScanResult(session.id)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.InvalidScanConfiguration::class.java)
    }

    @Test
    fun getScanResult_returnsTheFullReport_afterCompletion() = runTest {
        val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data
        repository.startScanSession(session.id)
        repository.completeScanSession(session.id, sampleStatistics(), emptyList())

        val result = repository.getScanResult(session.id)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).data.isClean).isTrue()
    }

    // --- observeScanHistory / getLatestScanResult ---

    @Test
    fun observeScanHistory_emitsANewEntry_whenAScanCompletes() = runTest {
        repository.observeScanHistory().test {
            assertThat(awaitItem()).isEmpty()

            val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data
            repository.startScanSession(session.id)
            repository.completeScanSession(session.id, sampleStatistics(), emptyList())

            val history = awaitItem()
            assertThat(history).hasSize(1)
            assertThat(history.first().session.id).isEqualTo(session.id)
        }
    }

    @Test
    fun getLatestScanResult_isNull_whenNothingHasCompletedYet() = runTest {
        val result = repository.getLatestScanResult()

        assertThat(result).isEqualTo(AppResult.Success(null))
    }

    @Test
    fun getLatestScanResult_returnsTheMostRecentlyCompletedScan() = runTest {
        val first = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data
        repository.startScanSession(first.id)
        repository.completeScanSession(first.id, sampleStatistics(), emptyList())

        val second = (repository.createScanSession(ScanType.FULL) as AppResult.Success).data
        repository.startScanSession(second.id)
        repository.completeScanSession(second.id, sampleStatistics(), emptyList())

        val result = repository.getLatestScanResult()

        assertThat((result as AppResult.Success).data?.session?.id).isEqualTo(second.id)
    }

    // --- deleteScanHistory / clearScanHistory ---

    @Test
    fun deleteScanHistory_removesTheSessionAndItsResult() = runTest {
        val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data
        repository.startScanSession(session.id)
        repository.completeScanSession(session.id, sampleStatistics(), emptyList())

        repository.deleteScanHistory(session.id)

        assertThat(repository.getScanSession(session.id))
            .isEqualTo(AppResult.Failure(AppError.ScanSessionNotFound(session.id)))
    }

    @Test
    fun clearScanHistory_removesEverySession() = runTest {
        val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data
        repository.startScanSession(session.id)
        repository.completeScanSession(session.id, sampleStatistics(), emptyList())

        repository.clearScanHistory()

        assertThat(repository.getLatestScanResult()).isEqualTo(AppResult.Success(null))
    }

    // --- getActiveScanSession ---

    @Test
    fun getActiveScanSession_isNull_whenNothingIsRunning() = runTest {
        assertThat(repository.getActiveScanSession()).isEqualTo(AppResult.Success(null))
    }

    @Test
    fun getActiveScanSession_returnsTheRunningSession() = runTest {
        val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data
        repository.startScanSession(session.id)

        val result = repository.getActiveScanSession()

        assertThat((result as AppResult.Success).data?.id).isEqualTo(session.id)
    }

    @Test
    fun getActiveScanSession_isNullAgain_onceTheSessionCompletes() = runTest {
        val session = (repository.createScanSession(ScanType.QUICK) as AppResult.Success).data
        repository.startScanSession(session.id)
        repository.completeScanSession(session.id, sampleStatistics(), emptyList())

        assertThat(repository.getActiveScanSession()).isEqualTo(AppResult.Success(null))
    }

    // --- observeScanProgress / updateScanProgress (in-memory, not Room) ---

    @Test
    fun observeScanProgress_emitsTheStartingSnapshotThenUpdates() = runTest {
        repository.observeScanProgress("session-1").test {
            assertThat(awaitItem()).isEqualTo(ScanProgress.starting("session-1"))

            repository.updateScanProgress(
                ScanProgress("session-1", itemsProcessed = 3, totalItems = 10, threatsFoundSoFar = 1),
            )

            assertThat(awaitItem().itemsProcessed).isEqualTo(3)
        }
    }
}
