package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.fake.FakeThreatDescriptionProvider
import com.space.antivirus.domain.scoring.HighestSeverityRiskScorer
import org.junit.Test

class BuildThreatUseCaseTest {

    private val analyzerId = AnalyzerId("test-analyzer")

    private fun detection(riskLevel: RiskLevel, threatType: ThreatType, id: String) = Detection(
        id = id,
        analyzerId = analyzerId,
        threatType = threatType,
        evidenceDescription = "evidence for $id",
        riskLevel = riskLevel,
    )

    @Test
    fun `builds a valid Threat from a Flagged outcome`() {
        val descriptionProvider = FakeThreatDescriptionProvider(
            title = "Unusual permission request",
            description = "This app requests SMS access unrelated to its stated purpose.",
        )
        val useCase = BuildThreatUseCase(HighestSeverityRiskScorer(), descriptionProvider)
        val outcome = AnalysisOutcome.Flagged(
            targetIdentifier = "com.example.suspicious",
            detections = listOf(
                detection(RiskLevel.ATTENTION, ThreatType.SUSPICIOUS_PERMISSION_USAGE, "d1"),
            ),
        )

        val threat = useCase(outcome, nowEpochMillis = 1_000L)

        assertThat(threat.targetIdentifier).isEqualTo("com.example.suspicious")
        assertThat(threat.riskLevel).isEqualTo(RiskLevel.ATTENTION)
        assertThat(threat.threatType).isEqualTo(ThreatType.SUSPICIOUS_PERMISSION_USAGE)
        assertThat(threat.title).isEqualTo("Unusual permission request")
        assertThat(threat.description).isEqualTo("This app requests SMS access unrelated to its stated purpose.")
        assertThat(threat.detections).containsExactly(
            detection(RiskLevel.ATTENTION, ThreatType.SUSPICIOUS_PERMISSION_USAGE, "d1"),
        )
        assertThat(threat.discoveredAtEpochMillis).isEqualTo(1_000L)
        assertThat(threat.id).isNotEmpty()
    }

    @Test
    fun `threatType and riskLevel are driven by the highest-severity detection`() {
        val useCase = BuildThreatUseCase(HighestSeverityRiskScorer(), FakeThreatDescriptionProvider())
        val outcome = AnalysisOutcome.Flagged(
            targetIdentifier = "file.apk",
            detections = listOf(
                detection(RiskLevel.INFO, ThreatType.UNKNOWN, "low"),
                detection(RiskLevel.ACTION_NEEDED, ThreatType.MALWARE, "high"),
                detection(RiskLevel.ATTENTION, ThreatType.POTENTIALLY_UNWANTED_APPLICATION, "mid"),
            ),
        )

        val threat = useCase(outcome)

        assertThat(threat.riskLevel).isEqualTo(RiskLevel.ACTION_NEEDED)
        assertThat(threat.threatType).isEqualTo(ThreatType.MALWARE)
    }

    @Test
    fun `description provider receives the same threatType and detections used to build the Threat`() {
        val descriptionProvider = FakeThreatDescriptionProvider()
        val useCase = BuildThreatUseCase(HighestSeverityRiskScorer(), descriptionProvider)
        val detections = listOf(detection(RiskLevel.ACTION_NEEDED, ThreatType.MALWARE, "d1"))
        val outcome = AnalysisOutcome.Flagged(targetIdentifier = "file.apk", detections = detections)

        val threat = useCase(outcome)

        assertThat(descriptionProvider.lastTitleRequestArgs).isEqualTo(ThreatType.MALWARE to detections)
        assertThat(descriptionProvider.lastDescriptionRequestArgs).isEqualTo(ThreatType.MALWARE to detections)
        assertThat(threat.threatType).isEqualTo(ThreatType.MALWARE)
    }

    @Test
    fun `generates a unique id per call`() {
        val useCase = BuildThreatUseCase(HighestSeverityRiskScorer(), FakeThreatDescriptionProvider())
        val outcome = AnalysisOutcome.Flagged(
            targetIdentifier = "file.apk",
            detections = listOf(detection(RiskLevel.ATTENTION, ThreatType.UNKNOWN, "d1")),
        )

        val first = useCase(outcome)
        val second = useCase(outcome)

        assertThat(first.id).isNotEqualTo(second.id)
    }
}
