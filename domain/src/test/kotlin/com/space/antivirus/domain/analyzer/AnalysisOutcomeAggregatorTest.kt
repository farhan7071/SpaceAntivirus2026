package com.space.antivirus.domain.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ThreatType
import org.junit.Test

class AnalysisOutcomeAggregatorTest {

    private val aggregator = AnalysisOutcomeAggregator()
    private val targetId = "com.example.app"

    private fun detection(id: String) = Detection(
        id = id,
        analyzerId = AnalyzerId("test"),
        threatType = ThreatType.UNKNOWN,
        evidenceDescription = "evidence",
        riskLevel = RiskLevel.ATTENTION,
    )

    @Test
    fun `all Clean outcomes aggregate to Clean`() {
        val result = aggregator.aggregate(
            listOf(AnalysisOutcome.Clean(targetId), AnalysisOutcome.Clean(targetId)),
        )
        assertThat(result).isEqualTo(AnalysisOutcome.Clean(targetId))
    }

    @Test
    fun `a single Flagged outcome among Clean outcomes wins`() {
        val flagged = AnalysisOutcome.Flagged(targetId, listOf(detection("d1")))
        val result = aggregator.aggregate(listOf(AnalysisOutcome.Clean(targetId), flagged))
        assertThat(result).isEqualTo(flagged)
    }

    @Test
    fun `multiple Flagged outcomes have their detections concatenated, not dropped`() {
        val first = AnalysisOutcome.Flagged(targetId, listOf(detection("d1")))
        val second = AnalysisOutcome.Flagged(targetId, listOf(detection("d2"), detection("d3")))

        val result = aggregator.aggregate(listOf(first, second)) as AnalysisOutcome.Flagged

        assertThat(result.detections.map { it.id }).containsExactly("d1", "d2", "d3")
    }

    @Test
    fun `Inconclusive wins over Clean when there is no Flagged outcome`() {
        val inconclusive = AnalysisOutcome.Inconclusive(targetId, "could not read file")
        val result = aggregator.aggregate(listOf(AnalysisOutcome.Clean(targetId), inconclusive))
        assertThat(result).isEqualTo(inconclusive)
    }

    @Test
    fun `Flagged wins over Inconclusive`() {
        val flagged = AnalysisOutcome.Flagged(targetId, listOf(detection("d1")))
        val inconclusive = AnalysisOutcome.Inconclusive(targetId, "could not read file")
        val result = aggregator.aggregate(listOf(inconclusive, flagged))
        assertThat(result).isEqualTo(flagged)
    }

    @Test
    fun `multiple Inconclusive reasons are combined, not dropped`() {
        val result = aggregator.aggregate(
            listOf(
                AnalysisOutcome.Inconclusive(targetId, "reason A"),
                AnalysisOutcome.Inconclusive(targetId, "reason B"),
            ),
        ) as AnalysisOutcome.Inconclusive

        assertThat(result.reason).contains("reason A")
        assertThat(result.reason).contains("reason B")
    }

    @Test
    fun `rejects an empty outcome list`() {
        val exception = runCatching { aggregator.aggregate(emptyList()) }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects outcomes for different targets`() {
        val exception = runCatching {
            aggregator.aggregate(
                listOf(AnalysisOutcome.Clean("target-a"), AnalysisOutcome.Clean("target-b")),
            )
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
