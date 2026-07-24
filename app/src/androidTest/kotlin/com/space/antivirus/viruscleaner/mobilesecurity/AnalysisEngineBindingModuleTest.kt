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
import com.space.antivirus.domain.scoring.RiskScorer
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
 * types at runtime. Updated in Sprint 014: the analyzer Set is no longer
 * empty now that SuspiciousPermissionPatternAnalyzer is registered, so
 * this test's assertions changed to match — a stale "the set is empty"
 * assertion would have been a real, silent regression if left unfixed.
 *
 * Deliberately does NOT attempt to inject RunScanRequestUseCase or
 * BuildThreatUseCase — those still can't be constructed, since
 * ThreatDescriptionProvider has no binding yet (Phase B, ADR 0016).
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

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun threatAnalyzerRegistry_injectsSuccessfully() {
        assertThat(threatAnalyzerRegistry).isNotNull()
    }

    @Test
    fun threatAnalyzerRegistry_containsTheRealAnalyzerRegisteredInSprint014() {
        val analyzers = threatAnalyzerRegistry.allAnalyzers()

        assertThat(analyzers).hasSize(1)
        assertThat(analyzers.first().id).isEqualTo(AnalyzerId("suspicious-permission-pattern"))
    }

    @Test
    fun threatAnalyzerRegistry_routesTheRealAnalyzerOnlyToApplicationTargets_notFileTargets() {
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
        assertThat(threatAnalyzerRegistry.analyzersFor(applicationTarget)).hasSize(1)
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
}
