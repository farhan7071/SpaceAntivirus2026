package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.analyzer.AnalysisOutcomeAggregator
import com.space.antivirus.domain.analyzer.AnalyzerExecutor
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
import com.space.antivirus.domain.analyzer.identifier
import com.space.antivirus.domain.fake.DelayingThreatAnalyzer
import com.space.antivirus.domain.fake.FakeEnumerationRepository
import com.space.antivirus.domain.fake.FakeSecurityRepository
import com.space.antivirus.domain.fake.FakeThreatAnalyzer
import com.space.antivirus.domain.fake.FakeThreatAnalyzerRegistry
import com.space.antivirus.domain.fake.FakeThreatDescriptionProvider
import com.space.antivirus.domain.scoring.HighestSeverityRiskScorer
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RunScanRequestUseCaseTest {

    private val fileTarget = ScanTarget.FileTarget(
        FileMetadata(
            path = "/downloads/file.apk",
            name = "file.apk",
            sizeBytes = 100L,
            mimeType = "application/vnd.android.package-archive",
            lastModifiedEpochMillis = 0L,
            isDirectory = false,
        ),
    )

    /**
     * Extension on TestScope specifically so every UseCase built here
     * shares runTest's own testScheduler — constructing an unrelated
     * `StandardTestDispatcher()` per UseCase would give each one its own
     * disconnected virtual-time scheduler, which runTest's auto-advancing
     * doesn't drive, and the test would hang rather than actually run.
     */
    private fun TestScope.buildUseCase(
        enumerationRepository: FakeEnumerationRepository,
        // Widened from List<FakeThreatAnalyzer> to List<ThreatAnalyzer>:
        // the cancellation test needs to pass a DelayingThreatAnalyzer
        // alongside FakeThreatAnalyzer elsewhere in this file. Both are
        // sibling ThreatAnalyzer implementations, not one a subtype of
        // the other, and Kotlin generics are invariant by default —
        // List<FakeThreatAnalyzer> could never accept a
        // List<DelayingThreatAnalyzer> regardless of a shared supertype.
        // FakeThreatAnalyzerRegistry already expects List<ThreatAnalyzer>
        // (see its own constructor), so this also removes a redundant,
        // overly-narrow type this helper never needed to declare.
        analyzers: List<ThreatAnalyzer>,
        securityRepository: FakeSecurityRepository = FakeSecurityRepository(),
    ): RunScanRequestUseCase {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = FakeThreatAnalyzerRegistry(analyzers)
        return RunScanRequestUseCase(
            resolveScanTargets = ResolveScanTargetsUseCase(enumerationRepository, dispatcher),
            analyzeScanTarget = AnalyzeScanTargetUseCase(registry, AnalyzerExecutor(), AnalysisOutcomeAggregator(), dispatcher),
            buildThreat = BuildThreatUseCase(HighestSeverityRiskScorer(), FakeThreatDescriptionProvider()),
            startScanSession = StartScanSessionUseCase(securityRepository, dispatcher),
            completeScanSession = CompleteScanSessionUseCase(securityRepository, dispatcher),
            securityRepository = securityRepository,
            dispatcher = dispatcher,
        )
    }

    private fun request(scopes: List<ScanScope>) = ScanRequest(
        id = "req-1",
        scanType = ScanType.QUICK,
        scopes = scopes,
        createdAtEpochMillis = 0L,
    )

    @Test
    fun `no applicable analyzers yields an honest fully-inconclusive result, not a false clean`() = runTest {
        val enumerationRepository = FakeEnumerationRepository(fileTargets = listOf(fileTarget))
        val useCase = buildUseCase(enumerationRepository, analyzers = emptyList())

        val result = useCase(request(listOf(ScanScope.DownloadsFolder)))

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val scanResult = (result as AppResult.Success).data
        assertThat(scanResult.isClean).isFalse()
        assertThat(scanResult.statistics.itemsInconclusive).isEqualTo(1)
        assertThat(scanResult.statistics.threatsFound).isEqualTo(0)
        assertThat(scanResult.threats).isEmpty()
    }

    @Test
    fun `a genuinely clean analyzer yields isClean true`() = runTest {
        val enumerationRepository = FakeEnumerationRepository(fileTargets = listOf(fileTarget))
        val cleanAnalyzer = FakeThreatAnalyzer(
            id = AnalyzerId("clean-analyzer"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Success(AnalysisOutcome.Clean(fileTarget.identifier)),
        )
        val useCase = buildUseCase(enumerationRepository, analyzers = listOf(cleanAnalyzer))

        val result = useCase(request(listOf(ScanScope.DownloadsFolder)))

        val scanResult = (result as AppResult.Success).data
        assertThat(scanResult.isClean).isTrue()
        assertThat(scanResult.statistics.itemsInconclusive).isEqualTo(0)
    }

    @Test
    fun `a flagged analyzer produces a persisted Threat`() = runTest {
        val enumerationRepository = FakeEnumerationRepository(fileTargets = listOf(fileTarget))
        val flaggingAnalyzer = FakeThreatAnalyzer(
            id = AnalyzerId("flagging"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Success(
                AnalysisOutcome.Flagged(
                    targetIdentifier = fileTarget.identifier,
                    detections = listOf(
                        Detection(
                            id = "d1",
                            analyzerId = AnalyzerId("flagging"),
                            threatType = ThreatType.MALWARE,
                            evidenceDescription = "matched a known-bad signature",
                            riskLevel = RiskLevel.ACTION_NEEDED,
                        ),
                    ),
                ),
            ),
        )
        val useCase = buildUseCase(enumerationRepository, analyzers = listOf(flaggingAnalyzer))

        val result = useCase(request(listOf(ScanScope.DownloadsFolder)))

        val scanResult = (result as AppResult.Success).data
        assertThat(scanResult.threats).hasSize(1)
        assertThat(scanResult.threats.first().threatType).isEqualTo(ThreatType.MALWARE)
        assertThat(scanResult.statistics.threatsFound).isEqualTo(1)
        assertThat(scanResult.isClean).isFalse()
    }

    @Test
    fun `publishes progress from starting through every target to completion`() = runTest {
        val securityRepository = FakeSecurityRepository()
        val secondFile = ScanTarget.FileTarget(
            FileMetadata(
                path = "/downloads/second.apk",
                name = "second.apk",
                sizeBytes = 50L,
                mimeType = "application/vnd.android.package-archive",
                lastModifiedEpochMillis = 0L,
                isDirectory = false,
            ),
        )
        val enumerationRepository = FakeEnumerationRepository(fileTargets = listOf(fileTarget, secondFile))
        val useCase = buildUseCase(enumerationRepository, analyzers = emptyList(), securityRepository = securityRepository)

        useCase(request(listOf(ScanScope.DownloadsFolder)))

        val progress = securityRepository.publishedProgress
        assertThat(progress.first().totalItems).isEqualTo(0) // the initial "starting" snapshot
        assertThat(progress[1].totalItems).isEqualTo(2) // right after enumeration resolves
        assertThat(progress[1].itemsProcessed).isEqualTo(0)
        assertThat(progress.last().itemsProcessed).isEqualTo(2)
        assertThat(progress.last().isComplete).isTrue()
    }

    @Test
    fun `a progress-update failure does not abort the scan`() = runTest {
        val securityRepository = FakeSecurityRepository().apply {
            forcedProgressUpdateFailure = AppError.Unexpected()
        }
        val enumerationRepository = FakeEnumerationRepository(fileTargets = listOf(fileTarget))
        val cleanAnalyzer = FakeThreatAnalyzer(
            id = AnalyzerId("clean-analyzer"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Success(AnalysisOutcome.Clean(fileTarget.identifier)),
        )
        val useCase = buildUseCase(enumerationRepository, listOf(cleanAnalyzer), securityRepository)

        val result = useCase(request(listOf(ScanScope.DownloadsFolder)))

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).data.isClean).isTrue()
        assertThat(securityRepository.publishedProgress).isEmpty() // every publish attempt failed, none recorded
    }

    @Test
    fun `cancelling the scan transitions the session to CANCELLED, not left stuck RUNNING`() = runTest {
        val securityRepository = FakeSecurityRepository()
        val secondFile = ScanTarget.FileTarget(
            FileMetadata(
                path = "/downloads/second.apk",
                name = "second.apk",
                sizeBytes = 50L,
                mimeType = "application/vnd.android.package-archive",
                lastModifiedEpochMillis = 0L,
                isDirectory = false,
            ),
        )
        val enumerationRepository = FakeEnumerationRepository(fileTargets = listOf(fileTarget, secondFile))
        // A real, unbounded suspension point is required — with no delay
        // anywhere, the whole scan runs to completion inside a single
        // runCurrent() call, leaving no window to cancel mid-flight.
        val slowAnalyzer = DelayingThreatAnalyzer(
            id = AnalyzerId("slow"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            delayMillis = 10_000,
            result = AppResult.Success(AnalysisOutcome.Clean(fileTarget.identifier)),
        )
        val useCase = buildUseCase(enumerationRepository, listOf(slowAnalyzer), securityRepository)

        val job = launch { useCase(request(listOf(ScanScope.DownloadsFolder))) }
        runCurrent() // let it start the session, resolve targets, and reach the analyzer's delay()
        job.cancel()
        job.join()

        val sessionId = securityRepository.publishedProgress.first().sessionId
        val session = securityRepository.getScanSession(sessionId)
        assertThat(session).isInstanceOf(AppResult.Success::class.java)
        assertThat((session as AppResult.Success).data.state).isEqualTo(ScanSessionState.CANCELLED)
    }

    @Test
    fun `enumeration failure aborts the run without starting analysis`() = runTest {
        val enumerationRepository = FakeEnumerationRepository(forcedFailure = AppError.StorageUnavailable)
        val useCase = buildUseCase(enumerationRepository, analyzers = emptyList())

        val result = useCase(request(listOf(ScanScope.DownloadsFolder)))

        assertThat(result).isEqualTo(AppResult.Failure(AppError.StorageUnavailable))
    }
}
