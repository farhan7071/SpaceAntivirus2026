package com.space.antivirus.core.analysisengine

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.analysisengine.analyzer.AppIdentityImpersonationAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.DebuggableApplicationAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.DeviceAdministratorAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.HighRiskPackageNameAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.OverlayPermissionAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.SuspiciousPermissionPatternAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.SurveillanceCombinationAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.UnknownInstallerSourceAnalyzer
import com.space.antivirus.core.analysisengine.reporting.ProductionThreatDescriptionProvider
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.analyzer.AnalysisOutcomeAggregator
import com.space.antivirus.domain.analyzer.AnalyzerExecutor
import com.space.antivirus.domain.analyzer.DefaultThreatAnalyzerRegistry
import com.space.antivirus.domain.scoring.CumulativeRiskScorer
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
        val target = appTarget(permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"))

        val outcome = (analyzeUseCase(target) as AppResult.Success).data as AnalysisOutcome.Flagged

        val threat = buildThreat(outcome, target)

        assertThat(threat.title).isEqualTo("Unusual permission combination")
        assertThat(threat.description).contains("SMS")
        assertThat(threat.description).contains("INTERNET")
        assertThat(threat.riskLevel).isEqualTo(RiskLevel.ATTENTION)
        assertThat(threat.threatType).isEqualTo(ThreatType.SUSPICIOUS_PERMISSION_USAGE)
        // Sprint 029 root-cause fix, verified end to end through the real
        // production pipeline, not just BuildThreatUseCaseTest's isolation.
        assertThat(threat.appLabel).isEqualTo("Example")
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
            val target = appTarget(
                appLabel = "WhatsApp",
                packageName = "com.definitely.not.whatsapp",
                permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
            )

            val outcome = (analyzeUseCase(target) as AppResult.Success).data as AnalysisOutcome.Flagged

            val threat = buildThreat(outcome, target)

            // One Threat, but both analyzers' evidence must be visible in
            // its description — the always-show-evidence rule doesn't stop
            // at whichever finding happened to drive the headline category.
            assertThat(threat.detections).hasSize(2)
            assertThat(threat.description).contains("SMS")
            assertThat(threat.description).contains("impersonating")
            // Sprint 029 root-cause fix: appLabel is the app's real name,
            // not the (deliberately fake, for this test) package name.
            assertThat(threat.appLabel).isEqualTo("WhatsApp")
        }

    @Test
    fun `an app triggering three independent Sprint 027 analyzers still produces exactly ONE Threat`() = runTest {
        // Directly demonstrates this sprint's own worked example (Chrome:
        // Dangerous Permission / Overlay / Unknown Installer -> one merged
        // result, not three separate ones): a single app matching
        // SurveillanceCombinationAnalyzer, OverlayPermissionAnalyzer, and
        // UnknownInstallerSourceAnalyzer simultaneously, run through the
        // real, full production pipeline with ALL eight production
        // analyzers registered — the same registry the real app builds,
        // not a hand-picked subset.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = DefaultThreatAnalyzerRegistry(
            setOf(
                SuspiciousPermissionPatternAnalyzer(),
                AppIdentityImpersonationAnalyzer(),
                OverlayPermissionAnalyzer(),
                SurveillanceCombinationAnalyzer(),
                DeviceAdministratorAnalyzer(),
                HighRiskPackageNameAnalyzer(),
                DebuggableApplicationAnalyzer(),
                UnknownInstallerSourceAnalyzer(),
            ),
        )
        val analyzeUseCase =
            AnalyzeScanTargetUseCase(registry, AnalyzerExecutor(), AnalysisOutcomeAggregator(), dispatcher)
        val buildThreat = BuildThreatUseCase(CumulativeRiskScorer(), ProductionThreatDescriptionProvider())

        val target = ScanTarget.ApplicationTarget(
            InstalledApplicationInfo(
                packageName = "com.example.suspicious",
                appLabel = "Example",
                versionName = "1.0",
                versionCode = 1L,
                installedAtEpochMillis = 0L,
                isSystemApp = false,
                apkPath = "/data/app/example.apk",
                requestedPermissions = listOf(
                    "android.permission.CAMERA",
                    "android.permission.RECORD_AUDIO",
                    "android.permission.SYSTEM_ALERT_WINDOW",
                    "android.permission.INTERNET",
                ),
                installerPackageName = null,
            ),
        )

        val outcome = (analyzeUseCase(target) as AppResult.Success).data as AnalysisOutcome.Flagged
        val threat = buildThreat(outcome, target)

        // Exactly one Threat for this app — the merging this sprint asked
        // for is a natural consequence of the existing per-target
        // aggregation architecture (ADR 0041), not new merging logic.
        assertThat(threat.detections).hasSize(3)
        val analyzerIds = threat.detections.map { it.analyzerId.value }.toSet()
        assertThat(analyzerIds).containsExactly(
            "surveillance-permission-combination",
            "overlay-permission-pattern",
            "unknown-installer-source",
        )

        // Two of the three (surveillance, overlay) are ATTENTION+MODERATE
        // from distinct analyzers — CumulativeRiskScorer escalates.
        assertThat(threat.riskLevel).isEqualTo(RiskLevel.ACTION_NEEDED)

        // Every reason shows up in the one combined description.
        assertThat(threat.description).contains("camera")
        assertThat(threat.description).contains("microphone")
        assertThat(threat.description).contains("draw over other apps")
        assertThat(threat.description).contains("unrecognized source")

        // Sprint 029 root-cause fix, end to end: this Threat's identity
        // is the app's real name, not just its generic threatType-derived
        // title — this is what makes three different apps sharing a
        // threatType distinguishable in a report, instead of looking like
        // the same finding repeated.
        assertThat(threat.appLabel).isEqualTo("Example")
    }

    @Test
    fun `Sprint 031 fix - the SAME permission combination from a trusted installer no longer escalates`() = runTest {
        // The exact regression this sprint exists to fix, demonstrated
        // against the real production pipeline: identical evidence to
        // the test directly above it, differing only in installerPackageName.
        // Communication/ride-sharing-style apps legitimately requesting
        // camera+microphone+overlay+internet, distributed through a real
        // app store, should no longer reach ACTION_NEEDED purely because
        // several ordinary permission-behavior analyzers agree — that
        // agreement is no longer strong enough on its own once a real,
        // on-device legitimacy signal is present.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val registry = DefaultThreatAnalyzerRegistry(
            setOf(
                SuspiciousPermissionPatternAnalyzer(),
                AppIdentityImpersonationAnalyzer(),
                OverlayPermissionAnalyzer(),
                SurveillanceCombinationAnalyzer(),
                DeviceAdministratorAnalyzer(),
                HighRiskPackageNameAnalyzer(),
                DebuggableApplicationAnalyzer(),
                UnknownInstallerSourceAnalyzer(),
            ),
        )
        val analyzeUseCase =
            AnalyzeScanTargetUseCase(registry, AnalyzerExecutor(), AnalysisOutcomeAggregator(), dispatcher)
        val buildThreat = BuildThreatUseCase(CumulativeRiskScorer(), ProductionThreatDescriptionProvider())

        val target = ScanTarget.ApplicationTarget(
            InstalledApplicationInfo(
                packageName = "com.example.communication",
                appLabel = "Example Communication App",
                versionName = "1.0",
                versionCode = 1L,
                installedAtEpochMillis = 0L,
                isSystemApp = false,
                apkPath = "/data/app/example.apk",
                requestedPermissions = listOf(
                    "android.permission.CAMERA",
                    "android.permission.RECORD_AUDIO",
                    "android.permission.SYSTEM_ALERT_WINDOW",
                    "android.permission.INTERNET",
                ),
                // The only change from the test above: a real, known,
                // on-device legitimacy signal.
                installerPackageName = "com.android.vending",
            ),
        )

        val outcome = (analyzeUseCase(target) as AppResult.Success).data as AnalysisOutcome.Flagged
        val threat = buildThreat(outcome, target)

        // Both surveillance and overlay still fire — evidence is
        // preserved, users are still informed (goal #3) — but both are
        // now LOW confidence, so neither qualifies to co-escalate.
        // UnknownInstallerSourceAnalyzer no longer fires at all, since
        // installerPackageName is no longer null.
        assertThat(threat.detections).hasSize(2)
        val analyzerIds = threat.detections.map { it.analyzerId.value }.toSet()
        assertThat(analyzerIds).containsExactly("surveillance-permission-combination", "overlay-permission-pattern")
        assertThat(threat.detections.map { it.confidence }).containsExactly(Confidence.LOW, Confidence.LOW)

        // The actual fix: ATTENTION, not ACTION_NEEDED.
        assertThat(threat.riskLevel).isEqualTo(RiskLevel.ATTENTION)

        // Evidence remains fully visible despite the lower confidence -
        // "preserve evidence, continue informing the user."
        assertThat(threat.description).contains("camera")
        assertThat(threat.description).contains("draw over other apps")
    }
}
