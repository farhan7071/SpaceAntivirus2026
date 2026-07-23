package com.space.antivirus.domain.usecase

import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.Threat
import com.space.antivirus.domain.reporting.ThreatDescriptionProvider
import com.space.antivirus.domain.scoring.RiskScorer
import java.util.UUID
import javax.inject.Inject

/**
 * Converts a Flagged AnalysisOutcome (Sprint 004C Patch 2) into a
 * persistable Threat (Sprint 004A's model) — the missing link Patch 1/2
 * deliberately deferred rather than inventing UX copy inside domain. See
 * ThreatDescriptionProvider's KDoc for why that stayed a contract.
 *
 * Typed to accept AnalysisOutcome.Flagged specifically, not the sealed
 * AnalysisOutcome supertype — Clean and Inconclusive outcomes have no
 * Detections to build a Threat from, and the type system should say so,
 * not a runtime check. Same reasoning as CreateScanRequestUseCase
 * (Sprint 004B): pure, synchronous construction with nothing that can
 * fail beyond what Threat's own `init` block already validates, so this
 * isn't built on the AppResult-wrapped UseCase base class.
 *
 * threatType is taken from whichever Detection drove the highest
 * riskLevel (ties broken by list order) — the same "report the worst
 * thing already found" philosophy as HighestSeverityRiskScorer, applied
 * to categorization instead of severity. riskLevel itself comes from the
 * injected RiskScorer, not computed locally, so swapping the scoring
 * strategy (ADR 0015) automatically changes this too.
 */
class BuildThreatUseCase @Inject constructor(
    private val riskScorer: RiskScorer,
    private val descriptionProvider: ThreatDescriptionProvider,
) {
    operator fun invoke(
        outcome: AnalysisOutcome.Flagged,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Threat {
        val drivingDetection = outcome.detections.maxBy { it.riskLevel }
        val threatType = drivingDetection.threatType

        return Threat(
            id = UUID.randomUUID().toString(),
            targetIdentifier = outcome.targetIdentifier,
            threatType = threatType,
            riskLevel = riskScorer.score(outcome.detections),
            title = descriptionProvider.titleFor(threatType, outcome.detections),
            description = descriptionProvider.descriptionFor(threatType, outcome.detections),
            detections = outcome.detections,
            discoveredAtEpochMillis = nowEpochMillis,
        )
    }
}
