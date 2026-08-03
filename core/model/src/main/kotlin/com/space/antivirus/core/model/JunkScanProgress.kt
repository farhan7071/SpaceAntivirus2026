package com.space.antivirus.core.model

/**
 * A real-time snapshot of an in-progress junk scan — Sprint 039.
 *
 * Deliberately carries **no total and no percentage.** A filesystem walk
 * does not know how many files it will visit until it has visited them;
 * producing a percentage would require either a full pre-pass (doubling
 * the I/O to display a number, for no functional gain) or an invented
 * denominator. Sprint 038 shipped an indeterminate scanning indicator
 * for exactly this reason, and this class keeps that honest: it reports
 * counts that are genuinely known at the moment of emission and nothing
 * else.
 *
 * `bytesFound` is the running total of junk identified so far, not of
 * bytes inspected.
 */
data class JunkScanProgress(
    val filesInspected: Int,
    val junkFound: Int,
    val bytesFound: Long,
    val currentPath: String?,
) {
    init {
        require(filesInspected >= 0) { "filesInspected cannot be negative" }
        require(junkFound >= 0) { "junkFound cannot be negative" }
        require(junkFound <= filesInspected) {
            "junkFound ($junkFound) cannot exceed filesInspected ($filesInspected)"
        }
        require(bytesFound >= 0) { "bytesFound cannot be negative" }
    }

    companion object {
        val STARTING = JunkScanProgress(filesInspected = 0, junkFound = 0, bytesFound = 0L, currentPath = null)
    }
}
