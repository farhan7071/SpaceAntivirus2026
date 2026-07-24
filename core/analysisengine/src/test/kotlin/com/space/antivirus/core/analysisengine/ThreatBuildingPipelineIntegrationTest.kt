package com.space.antivirus.core.analysisengine

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.analysisengine.analyzer.AppIdentityImpersonationAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.SuspiciousPermissionPatternAnalyzer
import com.space.antivirus.core.analysisengine.reporting.ProductionThreatDescriptionProvider
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.analyzer.AnalysisOutcomeAggregator
import com.space.antivirus.domain.analyzer.AnalyzerExecutor
import com.space.antivirus.domain.analyzer.DefaultThreatAnalyzerRegistry
import com.space.antivirus.domain.scoring.HighestSeverityRiskScorer
import com.space.antivirus.domain.usecase.AnalyzeScanTargetUseCase
import com.space.antivirus.domain.usecase.BuildThreatUseCase
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The success criterion this sprint was measured against, verified
 * directly: a Threat produced by the real, fully-wired production
 * pipeline — real analyzers, real registry, real executor, real
 * aggregator, real RiskScorer, real ThreatDescriptionProvider — now
 * carries real, non-placeholder title/description text. No fakes, no
 * Android needed, since every class involved is pure Kotlin.
 */
class ThreatBuildingPipelineIntegrationTest {

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

    @Test
    fun `a safe app produces a Clean outcome - no Threat is built, no placeholder text needed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = DefaultThreatAnalyzerRegistry(
            setOf(SuspiciousPermissionPatternAnalyzer(), AppIdentityImpersonationAnalyzer()),
        )
        val analyzeUseCase =
            AnalyzeScanTargetUseCase(registry, AnalyzerExecutor(), AnalysisOutcomeAggregator(), dispatcher)

        val result = analyzeUseCase(appTarget(permissions = listOf("android.permission.CAMERA")))

        // BuildThreatUseCase is only ever invoked for Flagged outcomes in
        // the real RunScanRequestUseCase (Sprint 004C onward) — a Clean
        // outcome never reaches it, confirmed here at the type level.
        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a suspicious app produces a real Threat with real, non-placeholder title and description`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = DefaultThreatAnalyzerRegistry(
            setOf(SuspiciousPermissionPatternAnalyzer(), AppIdentityImpersonationAnalyzer()),
        )
        val analyzeUseCase =
            AnalyzeScanTargetUseCase(registry, AnalyzerExecutor(), AnalysisOutcomeAggregator(), dispatcher)
        val buildThreat = BuildThreatUseCase(HighestSeverityRiskScorer(), ProductionThreatDescriptionProvider())

        val outcome = (
            analyzeUseCase(
                appTarget(permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET")),
            ) as AppResult.Success
            ).data as AnalysisOutcome.Flagged

        val threat = buildThreat(outcome)

        assertThat(threat.title).isEqualTo("Unusual permission combination")
        assertThat(threat.description).contains("SMS")
        assertThat(threat.description).contains("INTERNET")
        assertThat(threat.riskLevel).isEqualTo(RiskLevel.ATTENTION)
        assertThat(threat.threatType).isEqualTo(ThreatType.SUSPICIOUS_PERMISSION_USAGE)
    }

    @Test
    fun `both analyzers flagging the same app produces one Threat whose description shows both findings`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val registry = DefaultThreatAnalyzerRegistry(
                setOf(SuspiciousPermissionPatternAnalyzer(), AppIdentityImpersonationAnalyzer()),
            )
            val analyzeUseCase =
                AnalyzeScanTargetUseCase(registry, AnalyzerExecutor(), AnalysisOutcomeAggregator(), dispatcher)
            val buildThreat = BuildThreatUseCase(HighestSeverityRiskScorer(), ProductionThreatDescriptionProvider())

            val outcome = (
                analyzeUseCase(
                    appTarget(
                        appLabel = "WhatsApp",
                        packageName = "com.definitely.not.whatsapp",
                        permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
                    ),
                ) as AppResult.Success
                ).data as AnalysisOutcome.Flagged

            val threat = buildThreat(outcome)

            // One Threat, but both analyzers' evidence must be visible in
            // its description — the always-show-evidence rule doesn't stop
            // at whichever finding happened to drive the headline category.
            assertThat(threat.detections).hasSize(2)
            assertThat(threat.description).contains("SMS")
            assertThat(threat.description).contains("impersonating")
        }
}
