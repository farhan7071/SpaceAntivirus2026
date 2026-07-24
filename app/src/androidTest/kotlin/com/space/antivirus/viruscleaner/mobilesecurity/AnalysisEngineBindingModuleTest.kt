package com.space.antivirus.viruscleaner.mobilesecurity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
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
 * types at runtime. This is the one thing that could not be verified by
 * static reasoning alone: whether Dagger's compile-time graph validation
 * actually passes.
 *
 * Deliberately does NOT attempt to inject RunScanRequestUseCase or
 * BuildThreatUseCase — those still can't be constructed, since
 * ThreatDescriptionProvider has no binding yet (Phase B, ADR 0016). This
 * test's scope matches exactly what Sprint 013 actually closed, not more.
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
    fun threatAnalyzerRegistry_multibindsResolvesToAnEmptySet_notAMissingBindingError() {
        // The real point of @Multibinds: this call succeeding at all (not
        // throwing, not crashing at injection time) is what proves the
        // empty-Set case is a legitimate binding, not a missing one.
        assertThat(threatAnalyzerRegistry.allAnalyzers()).isEmpty()
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
