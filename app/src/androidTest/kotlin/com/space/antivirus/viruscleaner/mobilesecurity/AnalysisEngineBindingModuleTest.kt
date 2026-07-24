package com.space.antivirus.viruscleaner.mobilesecurity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.analyzer.ThreatAnalyzerRegistry
import com.space.antivirus.domain.reporting.ThreatDescriptionProvider
import com.space.antivirus.domain.scoring.RiskScorer
import com.space.antivirus.domain.usecase.RunScanRequestUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real verification that Sprint 013's two DI gaps are actually closed —
 * not just that AnalysisEngineBindingModule's @Binds/@Multibinds
 * declarations compile in isolation, but that Hilt's own annotation
 * processor accepts the FULL app graph and successfully injects both
 * types at runtime. Updated in Sprint 015: a second analyzer
 * (AppIdentityImpersonationAnalyzer) joined the registered set, so the
 * exact-count assertions changed again — same discipline as Sprint 014's
 * own update to this file, catching a stale assertion before it could
 * ship as a silent regression.
 *
 * Updated again in Sprint 016: now also injects ThreatDescriptionProvider
 * directly AND RunScanRequestUseCase itself — the real, concrete proof
 * that closing the last binding ADR 0026 left open makes the entire scan
 * pipeline Hilt-constructible for the first time in this project's
 * history. Every previous version of this test deliberately avoided
 * attempting that injection because it would have failed; now it's the
 * whole point of this update.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AnalysisEngineBindingModuleTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var threatAnalyzerRegistry: ThreatAnalyzerRegistry

    @Inject
    lateinit var riskScorer: RiskScorer

    @Inject
    lateinit var threatDescriptionProvider: ThreatDescriptionProvider

    @Inject
    lateinit var runScanRequestUseCase: RunScanRequestUseCase

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun threatAnalyzerRegistry_injectsSuccessfully() {
        assertThat(threatAnalyzerRegistry).isNotNull()
    }

    @Test
    fun threatAnalyzerRegistry_containsBothRealAnalyzers() {
        val analyzers = threatAnalyzerRegistry.allAnalyzers()

        assertThat(analyzers).hasSize(2)
        assertThat(analyzers.map { it.id }).containsExactly(
            AnalyzerId("suspicious-permission-pattern"),
            AnalyzerId("app-identity-impersonation"),
        )
    }

    @Test
    fun threatAnalyzerRegistry_routesBothRealAnalyzersOnlyToApplicationTargets_notFileTargets() {
        val fileTarget = ScanTarget.FileTarget(
            FileMetadata(
                path = "/downloads/file.txt",
                name = "file.txt",
                sizeBytes = 10L,
                mimeType = "text/plain",
                lastModifiedEpochMillis = 0L,
                isDirectory = false,
            ),
        )
        val applicationTarget = ScanTarget.ApplicationTarget(
            InstalledApplicationInfo(
                packageName = "com.example.app",
                appLabel = "Example",
                versionName = "1.0",
                versionCode = 1L,
                installedAtEpochMillis = 0L,
                isSystemApp = false,
                apkPath = "/data/app/example.apk",
                requestedPermissions = emptyList(),
            ),
        )

        assertThat(threatAnalyzerRegistry.analyzersFor(fileTarget)).isEmpty()
        assertThat(threatAnalyzerRegistry.analyzersFor(applicationTarget)).hasSize(2)
    }

    @Test
    fun riskScorer_injectsSuccessfully_andScoresARealDetectionList() {
        val detections = listOf(
            Detection(
                id = "d1",
                analyzerId = AnalyzerId("test-analyzer"),
                threatType = ThreatType.UNKNOWN,
                evidenceDescription = "test evidence",
                riskLevel = RiskLevel.ATTENTION,
            ),
        )

        assertThat(riskScorer.score(detections)).isEqualTo(RiskLevel.ATTENTION)
    }

    @Test
    fun threatDescriptionProvider_injectsSuccessfully_andProducesRealCopy() {
        val detections = listOf(
            Detection(
                id = "d1",
                analyzerId = AnalyzerId("test-analyzer"),
                threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                evidenceDescription = "test evidence",
                riskLevel = RiskLevel.ATTENTION,
            ),
        )

        val title = threatDescriptionProvider.titleFor(ThreatType.SUSPICIOUS_PERMISSION_USAGE, detections)

        assertThat(title).isNotEmpty()
    }

    @Test
    fun runScanRequestUseCase_isFullyHiltConstructible_forTheFirstTimeInThisProject() {
        // The real point of this test: successful injection into this
        // field (no exception during hiltRule.inject() in setUp()) IS the
        // verification. Every dependency this UseCase transitively needs —
        // SecurityRepository (Sprint 011), both real analyzers and the
        // registry (013/014/015), RiskScorer, and now
        // ThreatDescriptionProvider — must have resolved for this object
        // to exist at all.
        assertThat(runScanRequestUseCase).isNotNull()
    }
}
