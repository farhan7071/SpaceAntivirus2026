package com.space.antivirus.core.model

/**
 * Aggregate counters for a completed (or in-progress) ScanSession. Kept
 * as its own type (rather than folded into ScanSession) so a session's
 * lifecycle state and its numeric results are independently updatable —
 * single-responsibility split, not a stylistic preference.
 *
 * `itemsInconclusive` was added in Sprint 005 — a breaking change to this
 * already-shipped model, same category as ADR 0015's Detection.analyzerId
 * addition. Without it, a scan where every target came back
 * AnalysisOutcome.Inconclusive (e.g. because no analyzer is registered
 * yet for that target type) would report threatsFound=0 with no way to
 * distinguish that from a genuinely clean scan — exactly the false
 * reassurance AnalysisOutcomeAggregator's Inconclusive-beats-Clean
 * precedence rule (Sprint 004C) was designed to prevent. Losing that
 * distinction at the statistics layer would have quietly undone that
 * design one level up.
 */
data class ScanStatistics(
    val itemsScanned: Int,
    val threatsFound: Int,
    val itemsInconclusive: Int,
    val durationMillis: Long,
) {
    init {
        require(itemsScanned >= 0) { "itemsScanned cannot be negative" }
        require(threatsFound >= 0) { "threatsFound cannot be negative" }
        require(itemsInconclusive >= 0) { "itemsInconclusive cannot be negative" }
        require(threatsFound + itemsInconclusive <= itemsScanned || itemsScanned == 0) {
            "threatsFound ($threatsFound) + itemsInconclusive ($itemsInconclusive) cannot " +
                "exceed itemsScanned ($itemsScanned)"
        }
        require(durationMillis >= 0) { "durationMillis cannot be negative" }
    }

    companion object {
        /** For a session that hasn't produced any statistics yet
         *  (PENDING/RUNNING states). */
        val EMPTY = ScanStatistics(itemsScanned = 0, threatsFound = 0, itemsInconclusive = 0, durationMillis = 0)
    }
}
