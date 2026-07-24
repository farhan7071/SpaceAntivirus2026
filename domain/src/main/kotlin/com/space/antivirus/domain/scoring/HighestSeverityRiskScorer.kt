package com.space.antivirus.domain.scoring

import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import javax.inject.Inject

/**
 * The default, reference RiskScorer: a Threat's overall severity is the
 * highest severity among its individual Detections. This is a scoring
 * STRATEGY (how to summarize already-found evidence), not detection
 * logic (deciding whether something is a threat at all) — it doesn't
 * invent risk, weight unrelated signals, or exaggerate; it just reports
 * the worst thing already found, which is the most defensible default
 * per Sprint 002.75 §17 ("never exaggerate risk").
 *
 * Relies on RiskLevel's declared ordinal order being ascending severity —
 * see RiskLevel's own KDoc for why that's documented as meaningful there.
 *
 * The `@Inject` constructor was added in Sprint 013 — this class existed
 * since Sprint 004C but was never actually constructible by Hilt (no
 * annotated constructor at all), one of two real DI gaps found during
 * the Sprint 012 status review. See ADR 0026.
 */
class HighestSeverityRiskScorer @Inject constructor() : RiskScorer {

    override fun score(detections: List<Detection>): RiskLevel {
        require(detections.isNotEmpty()) {
            "Cannot score an empty detection list — a Threat with no " +
                "Detections shouldn't exist in the first place (see Threat's " +
                "own invariant)."
        }
        return detections.maxOf { it.riskLevel }
    }
}
