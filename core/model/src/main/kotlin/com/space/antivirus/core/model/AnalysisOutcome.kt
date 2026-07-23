package com.space.antivirus.core.model

/**
 * What one analyzer concluded about one ScanTarget. Three cases,
 * deliberately closed (sealed) so any future orchestration logic's `when`
 * over this type is compiler-checked for completeness, matching this
 * project's established AppResult/AppError pattern (ADR 0007).
 *
 * Inconclusive is not a failure (that's AppResult.Failure, at the
 * ThreatAnalyzer.analyze() call-site level) — it's a legitimate, honest
 * outcome meaning "this analyzer ran successfully but can't make a
 * determination", e.g. an encrypted file a signature engine can't read
 * the contents of. Collapsing that into Clean would be a false
 * reassurance; collapsing it into Flagged would be an unsupported claim —
 * both violate Sprint 002.75 §17 ("always show evidence" / "never
 * exaggerate").
 */
sealed interface AnalysisOutcome {
    val targetIdentifier: String

    data class Clean(
        override val targetIdentifier: String,
    ) : AnalysisOutcome

    data class Flagged(
        override val targetIdentifier: String,
        val detections: List<Detection>,
    ) : AnalysisOutcome {
        init {
            require(detections.isNotEmpty()) {
                "A Flagged outcome must be backed by at least one Detection — " +
                    "same evidence requirement as Threat itself."
            }
        }
    }

    data class Inconclusive(
        override val targetIdentifier: String,
        val reason: String,
    ) : AnalysisOutcome {
        init {
            require(reason.isNotBlank()) { "Inconclusive requires a specific, non-blank reason" }
        }
    }
}
