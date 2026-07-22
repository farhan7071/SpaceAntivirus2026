package com.space.antivirus.core.model

/**
 * Aggregate counters for a completed (or in-progress) ScanSession. Kept
 * as its own type (rather than folded into ScanSession) so a session's
 * lifecycle state and its numeric results are independently updatable —
 * single-responsibility split, not a stylistic preference.
 */
data class ScanStatistics(
    val itemsScanned: Int,
    val threatsFound: Int,
    val durationMillis: Long,
) {
    init {
        require(itemsScanned >= 0) { "itemsScanned cannot be negative" }
        require(threatsFound >= 0) { "threatsFound cannot be negative" }
        require(threatsFound <= itemsScanned || itemsScanned == 0) {
            "threatsFound ($threatsFound) cannot exceed itemsScanned ($itemsScanned)"
        }
        require(durationMillis >= 0) { "durationMillis cannot be negative" }
    }

    companion object {
        /** For a session that hasn't produced any statistics yet
         *  (PENDING/RUNNING states). */
        val EMPTY = ScanStatistics(itemsScanned = 0, threatsFound = 0, durationMillis = 0)
    }
}
