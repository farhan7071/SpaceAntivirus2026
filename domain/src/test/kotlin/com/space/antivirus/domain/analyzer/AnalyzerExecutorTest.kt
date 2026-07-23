package com.space.antivirus.domain.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.fake.FakeThreatAnalyzer
import com.space.antivirus.domain.fake.ThrowingThreatAnalyzer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AnalyzerExecutorTest {

    private val executor = AnalyzerExecutor()
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

    @Test
    fun `a successful analyzer produces metrics with succeeded true`() = runTest {
        val analyzer = FakeThreatAnalyzer(
            id = AnalyzerId("test"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Success(AnalysisOutcome.Clean(target.identifier)),
        )

        val outcome = executor.execute(analyzer, target)

        assertThat(outcome.result).isEqualTo(AppResult.Success(AnalysisOutcome.Clean(target.identifier)))
        assertThat(outcome.metrics.succeeded).isTrue()
        assertThat(outcome.metrics.analyzerId).isEqualTo(AnalyzerId("test"))
    }

    @Test
    fun `an analyzer returning Failure produces metrics with succeeded false, result passed through`() = runTest {
        val analyzer = FakeThreatAnalyzer(
            id = AnalyzerId("test"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
            result = AppResult.Failure(AppError.EngineUnavailable),
        )

        val outcome = executor.execute(analyzer, target)

        assertThat(outcome.result).isEqualTo(AppResult.Failure(AppError.EngineUnavailable))
        assertThat(outcome.metrics.succeeded).isFalse()
    }

    @Test
    fun `an analyzer that throws is caught and converted to a Failure, not propagated uncaught`() = runTest {
        val analyzer = ThrowingThreatAnalyzer(
            id = AnalyzerId("throws"),
            capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS),
        )

        val outcome = executor.execute(analyzer, target)

        assertThat(outcome.result).isInstanceOf(AppResult.Failure::class.java)
        val error = (outcome.result as AppResult.Failure).error
        assertThat(error).isInstanceOf(AppError.Unexpected::class.java)
        assertThat(outcome.metrics.succeeded).isFalse()
    }

    @Test
    fun `CancellationException is rethrown, never swallowed as a Failure`() = runTest {
        val cancellingAnalyzer = object : ThreatAnalyzer {
            override val id = AnalyzerId("cancelling")
            override val capabilities = setOf(AnalyzerCapability.FILE_ANALYSIS)
            override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
                throw CancellationException("simulated cancellation")
            }
        }

        val exception = runCatching { executor.execute(cancellingAnalyzer, target) }.exceptionOrNull()

        assertThat(exception).isInstanceOf(CancellationException::class.java)
    }
}
