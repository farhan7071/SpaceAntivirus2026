package com.space.antivirus.core.analysisengine

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.analysisengine.analyzer.AppIdentityImpersonationAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.SuspiciousPermissionPatternAnalyzer
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.analyzer.AnalysisOutcomeAggregator
import com.space.antivirus.domain.analyzer.AnalyzerExecutor
import com.space.antivirus.domain.analyzer.DefaultThreatAnalyzerRegistry
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
import com.space.antivirus.domain.usecase.AnalyzeScanTargetUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Real "pipeline integration" verification — not a domain-module test
 * using fakes (which would be the wrong dependency direction; :domain
 * cannot depend on this module), and not an instrumented test either:
 * every real production class involved is pure Kotlin with no
 * Android/Room dependency, so this runs as a genuine plain JVM unit test.
 *
 * Updated in Sprint 015 for both production analyzers running together —
 * `realUseCase()` now registers both, matching AnalysisEngineBindingModule's
 * actual production Set<ThreatAnalyzer> contents.
 */
class AnalysisPipelineIntegrationTest {

    /** Throws unconditionally — simulates a genuinely broken third-party
     *  analyzer implementation, not just a well-behaved Failure result.
     *  Local to this test file since domain's test-only ThrowingThreatAnalyzer
     *  lives in :domain's own test source set, not visible here. */
    private class ThrowingThreatAnalyzer(
        override val id: AnalyzerId,
        override val capabilities: Set<AnalyzerCapability>,
    ) : ThreatAnalyzer {
        override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
            throw IllegalStateException("Simulated analyzer crash")
        }
    }

    private fun appTarget(
        appLabel: String = "Example",
        packageName: String = "com.example.app",
        permissions: List<String> = emptyList(),
    ) = ScanTarget.ApplicationTarget(
        InstalledApplicationInfo(
            packageName = packageName,
            appLabel = appLabel,
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

    private fun realUseCase(
        dispatcher: CoroutineDispatcher,
        analyzers: Set<ThreatAnalyzer> = setOf(
            SuspiciousPermissionPatternAnalyzer(),
            AppIdentityImpersonationAnalyzer(),
        ),
    ): AnalyzeScanTargetUseCase {
        val registry = DefaultThreatAnalyzerRegistry(analyzers)
        return AnalyzeScanTargetUseCase(registry, AnalyzerExecutor(), AnalysisOutcomeAggregator(), dispatcher)
    }

    @Test
    fun `a safe app runs through both real analyzers to a Clean outcome`() = runTest {
        val useCase = realUseCase(StandardTestDispatcher(testScheduler))
        val target = appTarget(permissions = listOf("android.permission.CAMERA"))

        val result = useCase(target)

        val outcome = (result as AppResult.Success).data
        assertThat(outcome).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `only the permission analyzer's rule triggers, the identity analyzer contributes nothing`() = runTest {
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
    fun `both analyzers independently flag the same app, both findings appear in one aggregated outcome`() =
        runTest {
            val useCase = realUseCase(StandardTestDispatcher(testScheduler))
            // Triggers SuspiciousPermissionPatternAnalyzer's SMS+INTERNET rule
            // AND AppIdentityImpersonationAnalyzer's brand-mismatch rule at
            // the same time — a real, if constructed, dual-finding scenario.
            val target = appTarget(
                appLabel = "WhatsApp",
                packageName = "com.definitely.not.whatsapp",
                permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
            )

            val result = useCase(target)

            val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
            // Two genuinely different findings (different threatType,
            // different evidence) — the aggregator's deduplication only
            // collapses EXACT matches, so both are correctly preserved.
            assertThat(outcome.detections).hasSize(2)
            val analyzerIds = outcome.detections.map { it.analyzerId }
            assertThat(analyzerIds).containsExactly(
                AnalyzerId("suspicious-permission-pattern"),
                AnalyzerId("app-identity-impersonation"),
            )
        }

    @Test
    fun `a broken third analyzer does not prevent the two real analyzers from contributing their findings`() =
        runTest {
            val brokenAnalyzer = ThrowingThreatAnalyzer(
                id = AnalyzerId("broken-third-party-analyzer"),
                capabilities = setOf(AnalyzerCapability.APPLICATION_ANALYSIS),
            )
            val useCase = realUseCase(
                StandardTestDispatcher(testScheduler),
                analyzers = setOf(
                    SuspiciousPermissionPatternAnalyzer(),
                    AppIdentityImpersonationAnalyzer(),
                    brokenAnalyzer,
                ),
            )
            val target = appTarget(
                permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
            )

            val result = useCase(target)

            // Fault isolation (ADR 0019) holding with two REAL production
            // analyzers in the mix, not just fakes — the broken analyzer's
            // crash is caught by AnalyzerExecutor and excluded, while the
            // working permission analyzer's finding still comes through.
            val outcome = (result as AppResult.Success).data
            assertThat(outcome).isInstanceOf(AnalysisOutcome.Flagged::class.java)
            assertThat((outcome as AnalysisOutcome.Flagged).detections).hasSize(1)
        }

    @Test
    fun `the real registry correctly routes a FileTarget away from both analyzers, yielding Inconclusive`() =
        runTest {
            val useCase = realUseCase(StandardTestDispatcher(testScheduler))

            val result = useCase(fileTarget())

            // No FILE_ANALYSIS-capable analyzer is registered at all — the
            // real, honest "nothing is looking at this" outcome (Sprint
            // 004C), not a false Clean.
            val outcome = (result as AppResult.Success).data
            assertThat(outcome).isInstanceOf(AnalysisOutcome.Inconclusive::class.java)
        }
}
