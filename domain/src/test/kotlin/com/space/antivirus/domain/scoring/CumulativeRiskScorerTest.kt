package com.space.antivirus.domain.scoring

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ThreatType
import org.junit.Test

class CumulativeRiskScorerTest {

    private val scorer = CumulativeRiskScorer()

    private fun detection(
        analyzerId: String,
        riskLevel: RiskLevel,
        confidence: Confidence,
    ) = Detection(
        id = "$analyzerId-detection",
        analyzerId = AnalyzerId(analyzerId),
        threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
        evidenceDescription = "evidence from $analyzerId",
        riskLevel = riskLevel,
        confidence = confidence,
    )

    @Test
    fun `a single ATTENTION detection stays ATTENTION - matches the sprint's own two-unrelated-warnings framing`() {
        val result = scorer.score(listOf(detection("analyzer-a", RiskLevel.ATTENTION, Confidence.HIGH)))

        assertThat(result).isEqualTo(RiskLevel.ATTENTION)
    }

    @Test
    fun `two independent ATTENTION plus MODERATE-confidence detections escalate to ACTION_NEEDED`() {
        val detections = listOf(
            detection("overlay-permission-pattern", RiskLevel.ATTENTION, Confidence.MODERATE),
            detection("device-administrator-standalone", RiskLevel.ATTENTION, Confidence.MODERATE),
        )

        val result = scorer.score(detections)

        assertThat(result).isEqualTo(RiskLevel.ACTION_NEEDED)
    }

    @Test
    fun `two detections from the SAME analyzer do not escalate - independence means distinct analyzers`() {
        val detections = listOf(
            detection("same-analyzer", RiskLevel.ATTENTION, Confidence.HIGH),
            detection("same-analyzer", RiskLevel.ATTENTION, Confidence.HIGH),
        )

        val result = scorer.score(detections)

        assertThat(result).isEqualTo(RiskLevel.ATTENTION)
    }

    @Test
    fun `one ATTENTION plus one LOW-confidence ATTENTION does not escalate`() {
        val detections = listOf(
            detection("analyzer-a", RiskLevel.ATTENTION, Confidence.HIGH),
            detection("analyzer-b", RiskLevel.ATTENTION, Confidence.LOW),
        )

        val result = scorer.score(detections)

        assertThat(result).isEqualTo(RiskLevel.ATTENTION)
    }

    @Test
    fun `two INFO-level detections do not escalate - even from distinct analyzers, even at HIGH confidence`() {
        val detections = listOf(
            detection("device-administrator-standalone", RiskLevel.INFO, Confidence.HIGH),
            detection("debuggable-application", RiskLevel.INFO, Confidence.HIGH),
        )

        val result = scorer.score(detections)

        assertThat(result).isEqualTo(RiskLevel.INFO)
    }

    @Test
    fun `an already ACTION_NEEDED detection stays ACTION_NEEDED regardless of what else is present`() {
        val detections = listOf(
            detection("analyzer-a", RiskLevel.ACTION_NEEDED, Confidence.LOW),
        )

        val result = scorer.score(detections)

        assertThat(result).isEqualTo(RiskLevel.ACTION_NEEDED)
    }

    @Test
    fun `an INFO detection alongside a qualifying pair does not prevent escalation`() {
        val detections = listOf(
            detection("analyzer-a", RiskLevel.ATTENTION, Confidence.MODERATE),
            detection("analyzer-b", RiskLevel.ATTENTION, Confidence.MODERATE),
            detection("analyzer-c", RiskLevel.INFO, Confidence.LOW),
        )

        val result = scorer.score(detections)

        assertThat(result).isEqualTo(RiskLevel.ACTION_NEEDED)
    }

    @Test
    fun `rejects an empty detection list`() {
        val exception = runCatching { scorer.score(emptyList()) }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
