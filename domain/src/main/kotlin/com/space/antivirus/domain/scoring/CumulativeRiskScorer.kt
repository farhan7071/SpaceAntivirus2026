package com.space.antivirus.domain.scoring

import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import javax.inject.Inject

/**
 * Sprint 027 — the scoring strategy RiskScorer's own KDoc anticipated
 * back in Sprint 004C ("a scoring strategy that weighs analyzer
 * confidence, once that concept exists"). Now the default binding
 * (AnalysisEngineBindingModule), replacing HighestSeverityRiskScorer —
 * which stays in this project, unchanged and still fully valid, as a
 * genuine alternative implementation of the same interface, not dead
 * code; that's the whole point of RiskScorer being an interface.
 *
 * THE RULE, stated precisely because "cumulative scoring" alone isn't
 * specific enough to be testable or defensible:
 *
 * 1. Start from the highest individual RiskLevel already present — this
 *    scorer never scores BELOW what any single Detection alone already
 *    justifies. If that's already ACTION_NEEDED, nothing to escalate.
 * 2. Otherwise, escalate to ACTION_NEEDED only if TWO OR MORE DISTINCT
 *    analyzers (by analyzerId, not detection count) each contributed a
 *    Detection that is BOTH at least ATTENTION severity AND at least
 *    MODERATE confidence.
 *
 * Every part of that second condition is deliberate:
 * - DISTINCT ANALYZERS, not detection count: this is what actually
 *   matches the sprint's own framing ("Accessibility + Overlay" — two
 *   independent signals, not one analyzer being verbose). A single
 *   analyzer producing two Detections about the same underlying finding
 *   shouldn't be able to escalate itself.
 * - At least ATTENTION severity: an INFO-level finding (the standalone
 *   DeviceAdministratorAnalyzer/DebuggableApplicationAnalyzer/
 *   UnknownInstallerSourceAnalyzer findings, Sprint 027) contributing
 *   toward escalation would mean genuinely weak, "just for awareness"
 *   evidence could push a Threat to the highest severity tier — exactly
 *   the "never exaggerate risk" violation (Sprint 002.75 §17) this
 *   project has avoided since its first analyzer.
 * - At least MODERATE confidence: same reasoning, the other axis — a
 *   LOW-confidence finding shouldn't be able to co-sign an escalation
 *   either, even if its severity happened to be ATTENTION. Confidence
 *   and severity both have to clear the bar independently.
 *
 * This never invents evidence, weighs unrelated signals against each
 * other unfairly, or turns "two things worth reviewing" into "certain
 * danger" — it recognizes that two INDEPENDENT, EACH-ALREADY-MEANINGFUL
 * signals on the SAME app are collectively stronger evidence than either
 * alone, which is a defensible, explainable escalation, not an invented
 * one.
 */
class CumulativeRiskScorer @Inject constructor() : RiskScorer {

    override fun score(detections: List<Detection>): RiskLevel {
        require(detections.isNotEmpty()) {
            "Cannot score an empty detection list — a Threat with no " +
                "Detections shouldn't exist in the first place (see Threat's " +
                "own invariant)."
        }

        val baseLevel = detections.maxOf { it.riskLevel }
        if (baseLevel == RiskLevel.ACTION_NEEDED) {
            return RiskLevel.ACTION_NEEDED
        }

        val independentQualifyingAnalyzers = detections
            .asSequence()
            .filter { it.riskLevel >= RiskLevel.ATTENTION && it.confidence >= Confidence.MODERATE }
            .map { it.analyzerId }
            .distinct()
            .count()

        return if (independentQualifyingAnalyzers >= MIN_INDEPENDENT_SIGNALS_TO_ESCALATE) {
            RiskLevel.ACTION_NEEDED
        } else {
            baseLevel
        }
    }

    private companion object {
        const val MIN_INDEPENDENT_SIGNALS_TO_ESCALATE = 2
    }
}
