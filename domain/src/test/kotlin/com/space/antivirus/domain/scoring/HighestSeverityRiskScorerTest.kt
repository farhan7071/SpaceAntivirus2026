package com.space.antivirus.domain.scoring

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ThreatType
import org.junit.Test

class HighestSeverityRiskScorerTest {

    private val scorer = HighestSeverityRiskScorer()
    private val analyzerId = AnalyzerId("test-analyzer")

    private fun detection(riskLevel: RiskLevel) = Detection(
        id = "d-${riskLevel.name}",
        analyzerId = analyzerId,
        threatType = ThreatType.UNKNOWN,
        evidenceDescription = "test evidence",
        riskLevel = riskLevel,
    )

    @Test
    fun `returns the single detection's risk level`() {
        val result = scorer.score(listOf(detection(RiskLevel.ATTENTION)))
        assertThat(result).isEqualTo(RiskLevel.ATTENTION)
    }

    @Test
    fun `returns the highest severity among multiple detections`() {
        val result = scorer.score(
            listOf(
                detection(RiskLevel.INFO),
                detection(RiskLevel.ACTION_NEEDED),
                detection(RiskLevel.ATTENTION),
            ),
        )
        assertThat(result).isEqualTo(RiskLevel.ACTION_NEEDED)
    }

    @Test
    fun `order of detections in the list does not affect the result`() {
        val ascending = scorer.score(listOf(detection(RiskLevel.INFO), detection(RiskLevel.ACTION_NEEDED)))
        val descending = scorer.score(listOf(detection(RiskLevel.ACTION_NEEDED), detection(RiskLevel.INFO)))
        assertThat(ascending).isEqualTo(descending)
    }

    @Test
    fun `rejects an empty detection list`() {
        val exception = runCatching { scorer.score(emptyList()) }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
