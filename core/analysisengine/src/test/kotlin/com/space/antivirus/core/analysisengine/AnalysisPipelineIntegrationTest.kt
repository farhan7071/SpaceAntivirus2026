package com.space.antivirus.core.analysisengine

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.analysisengine.analyzer.SuspiciousPermissionPatternAnalyzer
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.analyzer.AnalysisOutcomeAggregator
import com.space.antivirus.domain.analyzer.AnalyzerExecutor
import com.space.antivirus.domain.analyzer.DefaultThreatAnalyzerRegistry
import com.space.antivirus.domain.usecase.AnalyzeScanTargetUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Real "pipeline integration" verification, per Sprint 014's deliverable
 * requirements — not a domain-module test using fakes (which would be
 * the wrong dependency direction; :domain cannot depend on this module),
 * and not an instrumented test either: DefaultThreatAnalyzerRegistry,
 * AnalyzerExecutor, AnalysisOutcomeAggregator, AnalyzeScanTargetUseCase,
 * and SuspiciousPermissionPatternAnalyzer are all pure Kotlin with no
 * Android/Room dependency, so this runs as a genuine plain JVM unit test
 * exercising the REAL production classes together — the first time in
 * this project that's been possible for anything beyond a single class.
 */
class AnalysisPipelineIntegrationTest {

    private fun appTarget(permissions: List<String>) = ScanTarget.ApplicationTarget(
        InstalledApplicationInfo(
            packageName = "com.example.app",
            appLabel = "Example",
            versionName = "1.0",
            versionCode = 1L,
            installedAtEpochMillis = 0L,
            isSystemApp = false,
            apkPath = "/data/app/example.apk",
            requestedPermissions = permissions,
        ),
    )

    private fun fileTarget() = ScanTarget.FileTarget(
        FileMetadata(
            path = "/downloads/file.txt",
            name = "file.txt",
            sizeBytes = 10L,
            mimeType = "text/plain",
            lastModifiedEpochMillis = 0L,
            isDirectory = false,
        ),
    )

    private fun realUseCase(dispatcher: CoroutineDispatcher): AnalyzeScanTargetUseCase {
        val registry = DefaultThreatAnalyzerRegistry(setOf(SuspiciousPermissionPatternAnalyzer()))
        return AnalyzeScanTargetUseCase(registry, AnalyzerExecutor(), AnalysisOutcomeAggregator(), dispatcher)
    }

    @Test
    fun `a safe app runs through the real registry and executor to a Clean outcome`() = runTest {
        val useCase = realUseCase(StandardTestDispatcher(testScheduler))
        val target = appTarget(permissions = listOf("android.permission.CAMERA"))

        val result = useCase(target)

        val outcome = (result as AppResult.Success).data
        assertThat(outcome).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a suspicious app runs through the real registry, executor, and aggregator to a Flagged outcome`() =
        runTest {
            val useCase = realUseCase(StandardTestDispatcher(testScheduler))
            val target = appTarget(
                permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
            )

            val result = useCase(target)

            val outcome = (result as AppResult.Success).data
            assertThat(outcome).isInstanceOf(AnalysisOutcome.Flagged::class.java)
            assertThat((outcome as AnalysisOutcome.Flagged).detections).hasSize(1)
        }

    @Test
    fun `the real registry correctly routes a FileTarget away from this analyzer, yielding Inconclusive`() =
        runTest {
            val useCase = realUseCase(StandardTestDispatcher(testScheduler))

            val result = useCase(fileTarget())

            // No FILE_ANALYSIS-capable analyzer is registered at all — the
            // real, honest "nothing is looking at this" outcome (Sprint
            // 004C), not a false Clean. Confirms the registry's capability
            // routing works with a real analyzer in the set, not just in
            // isolation (Sprint 013's own registry tests already covered
            // the routing logic itself).
            val outcome = (result as AppResult.Success).data
            assertThat(outcome).isInstanceOf(AnalysisOutcome.Inconclusive::class.java)
        }
}
