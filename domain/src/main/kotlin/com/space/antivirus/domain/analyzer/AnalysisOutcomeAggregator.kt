package com.space.antivirus.domain.analyzer

import com.space.antivirus.core.model.AnalysisOutcome

/**
 * Combines multiple analyzers' outcomes for the SAME target into one
 * honest verdict. The rule, in order of precedence:
 *
 * 1. Any Flagged outcome -> Flagged, with every Flagged outcome's
 *    Detections concatenated (never dropped — losing a Detection because
 *    another analyzer disagreed would be exactly the "hide evidence"
 *    failure Sprint 002.75 §17 prohibits).
 * 2. No Flagged, but any Inconclusive -> Inconclusive. Even if every
 *    other analyzer said Clean, this app can't honestly claim "no
 *    threats found" when part of the analysis didn't reach a
 *    conclusion — that would be a false reassurance, not just an
 *    optimistic rounding.
 * 3. Every outcome Clean -> Clean.
 *
 * This is aggregation policy, not detection policy — it never invents a
 * verdict beyond what the individual analyzers already returned.
 *
 * DEDUPLICATION (Sprint 015): concatenated Detections are deduplicated by
 * exact (threatType, riskLevel, evidenceDescription) match before being
 * placed into the final Flagged outcome. This does NOT contradict the
 * "never drop evidence" rule above — that rule is about never discarding
 * a Detection because a DIFFERENT analyzer disagreed or found something
 * else; deduplication only ever collapses detections that are, in
 * substance, saying the exact same thing. Two analyzers independently
 * reaching an identical conclusion isn't two pieces of evidence, it's one
 * piece of evidence confirmed twice — showing it as two identical rows
 * would misrepresent the finding's actual weight, not just look
 * redundant. The FIRST occurrence's Detection (with its own id and
 * analyzerId) is kept; later exact duplicates are dropped.
 */
class AnalysisOutcomeAggregator {

    fun aggregate(outcomes: List<AnalysisOutcome>): AnalysisOutcome {
        require(outcomes.isNotEmpty()) { "Cannot aggregate an empty outcome list" }
        val targetIdentifier = outcomes.first().targetIdentifier
        require(outcomes.all { it.targetIdentifier == targetIdentifier }) {
            "All outcomes being aggregated must be for the same target " +
                "(found: ${outcomes.map { it.targetIdentifier }.distinct()})"
        }

        val flagged = outcomes.filterIsInstance<AnalysisOutcome.Flagged>()
        if (flagged.isNotEmpty()) {
            val deduplicatedDetections = flagged
                .flatMap { it.detections }
                .distinctBy { Triple(it.threatType, it.riskLevel, it.evidenceDescription) }
            return AnalysisOutcome.Flagged(
                targetIdentifier = targetIdentifier,
                detections = deduplicatedDetections,
            )
        }

        val inconclusive = outcomes.filterIsInstance<AnalysisOutcome.Inconclusive>()
        if (inconclusive.isNotEmpty()) {
            return AnalysisOutcome.Inconclusive(
                targetIdentifier = targetIdentifier,
                reason = inconclusive.joinToString(separator = "; ") { it.reason },
            )
        }

        return AnalysisOutcome.Clean(targetIdentifier = targetIdentifier)
    }
}
