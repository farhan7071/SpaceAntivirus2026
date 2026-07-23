package com.space.antivirus.core.model

/**
 * How long one ThreatAnalyzer took to run against one target, and
 * whether it produced a usable result. Exists so fault isolation
 * (Sprint 006) has something concrete to base "this analyzer is
 * unreliable/slow" observability on later — not surfaced anywhere yet
 * (no UI changes in this sprint), but the shape is real and testable now.
 */
data class AnalyzerExecutionMetrics(
    val analyzerId: AnalyzerId,
    val durationMillis: Long,
    val succeeded: Boolean,
) {
    init {
        require(durationMillis >= 0) { "durationMillis cannot be negative" }
    }
}
