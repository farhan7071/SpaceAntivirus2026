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
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.analyzer.AnalysisOutcomeAggregator
import com.space.antivirus.domain.analyzer.AnalyzerExecutor
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
import com.space.antivirus.domain.analyzer.identifier
import com.space.antivirus.domain.fake.FakeThreatAnalyzer
import com.space.antivirus.domain.fake.FakeThreatAnalyzerRegistry
import com.space.antivirus.domain.fake.ThrowingThreatAnalyzer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AnalyzeScanTargetUseCaseTest {

    private val target = ScanTarget.FileTarget(
        FileMetadata(
            path = "/downloads/file.apk",
            name = "file.apk",
            sizeBytes = 100L,
            mimeType = "application/vnd.android.package-archive",
            lastModifiedEpochMillis = 0L,
            isDirectory = false,
        ),
    )

    private fun useCaseFor(analyzers: List<ThreatAnalyzer>, testScheduler: TestCoroutineScheduler) =
        AnalyzeScanTargetUseCase(
            FakeThreatAnalyzerRegistry(analyzers),
            AnalyzerExecutor(),
            AnalysisOutcomeAggregator(),
            StandardTestDispatcher(testScheduler),
        )

    @Test
    fun `no registered analyzers yields an honest Inconclusive, not a false Clean`() = runTest {
        val useCase = useCaseFor(emptyList(), testScheduler)

        val result = useCase(target)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val outcome = (result as AppResult.Success).data
        assertThat(outcome).isInstanceOf(AnalysisOutcome.Inconclusive::class.java)
    }

    @Test
    fun `a single Clean analyzer yields Clean`() = runTest {
        val analyzer = FakeThreatAnalyzer(
            id = AnalyzerId("test-1"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Success(AnalysisOutcome.Clean(target.identifier)),
        )
        val useCase = useCaseFor(listOf(analyzer), testScheduler)

        val result = useCase(target)

        assertThat(result).isEqualTo(AppResult.Success(AnalysisOutcome.Clean(target.identifier)))
    }

    @Test
    fun `when it is the only analyzer, its failure propagates directly`() = runTest {
        val analyzer = FakeThreatAnalyzer(
            id = AnalyzerId("test-1"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Failure(AppError.EngineUnavailable),
        )
        val useCase = useCaseFor(listOf(analyzer), testScheduler)

        val result = useCase(target)

        assertThat(result).isEqualTo(AppResult.Failure(AppError.EngineUnavailable))
    }

    @Test
    fun `multiple successful analyzers are combined through the aggregator`() = runTest {
        val flaggingAnalyzer = FakeThreatAnalyzer(
            id = AnalyzerId("flagging"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Success(
                AnalysisOutcome.Flagged(
                    targetIdentifier = target.identifier,
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
        val cleanAnalyzer = FakeThreatAnalyzer(
            id = AnalyzerId("clean"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Success(AnalysisOutcome.Clean(target.identifier)),
        )
        val useCase = useCaseFor(listOf(flaggingAnalyzer, cleanAnalyzer), testScheduler)

        val result = useCase(target)

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val outcome = (result as AppResult.Success).data
        assertThat(outcome).isInstanceOf(AnalysisOutcome.Flagged::class.java)
        assertThat((outcome as AnalysisOutcome.Flagged).detections).hasSize(1)
    }

    @Test
    fun `fault isolation - one analyzer failing does not prevent another from contributing`() = runTest {
        val brokenAnalyzer = FakeThreatAnalyzer(
            id = AnalyzerId("broken"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Failure(AppError.EngineUnavailable),
        )
        val workingAnalyzer = FakeThreatAnalyzer(
            id = AnalyzerId("working"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Success(AnalysisOutcome.Clean(target.identifier)),
        )
        val useCase = useCaseFor(listOf(brokenAnalyzer, workingAnalyzer), testScheduler)

        val result = useCase(target)

        assertThat(result).isEqualTo(AppResult.Success(AnalysisOutcome.Clean(target.identifier)))
    }

    @Test
    fun `fault isolation - an analyzer that throws does not crash the whole target's analysis`() = runTest {
        val throwingAnalyzer = ThrowingThreatAnalyzer(
            id = AnalyzerId("throws"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
        )
        val workingAnalyzer = FakeThreatAnalyzer(
            id = AnalyzerId("working"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Success(AnalysisOutcome.Clean(target.identifier)),
        )
        val useCase = useCaseFor(listOf(throwingAnalyzer, workingAnalyzer), testScheduler)

        val result = useCase(target)

        assertThat(result).isEqualTo(AppResult.Success(AnalysisOutcome.Clean(target.identifier)))
    }

    @Test
    fun `when every analyzer fails, the first failure is surfaced rather than a false Inconclusive`() = runTest {
        val firstBroken = FakeThreatAnalyzer(
            id = AnalyzerId("broken-1"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Failure(AppError.EngineUnavailable),
        )
        val secondBroken = FakeThreatAnalyzer(
            id = AnalyzerId("broken-2"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Failure(AppError.Unexpected()),
        )
        val useCase = useCaseFor(listOf(firstBroken, secondBroken), testScheduler)

        val result = useCase(target)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
    }
}
