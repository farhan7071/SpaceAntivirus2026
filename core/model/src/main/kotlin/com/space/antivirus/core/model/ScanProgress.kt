package com.space.antivirus.core.model

/**
 * A real-time snapshot of an in-progress scan — what a future progress UI
 * would collect from SecurityRepository.observeScanProgress() to render
 * "scanning 47 of 200" (or similar) while RunScanRequestUseCase is still
 * running. Named in this project's very first architecture planning pass
 * alongside ScanSession/ScanStatistics but not actually built until now,
 * since nothing needed to observe mid-scan state before the orchestration
 * pipeline (Feature Block 1) existed to report it.
 *
 * `totalItems == 0` is a valid, meaningful state — not an error — for the
 * brief window between a session starting and enumeration finishing
 * resolving how many targets there actually are. See `starting()`.
 */
data class ScanProgress(
    val sessionId: String,
    val itemsProcessed: Int,
    val totalItems: Int,
    val threatsFoundSoFar: Int,
) {
    init {
        require(itemsProcessed >= 0) { "itemsProcessed cannot be negative" }
        require(totalItems >= 0) { "totalItems cannot be negative" }
        require(itemsProcessed <= totalItems || totalItems == 0) {
            "itemsProcessed ($itemsProcessed) cannot exceed totalItems ($totalItems)"
        }
        require(threatsFoundSoFar >= 0) { "threatsFoundSoFar cannot be negative" }
        require(threatsFoundSoFar <= itemsProcessed) {
            "threatsFoundSoFar ($threatsFoundSoFar) cannot exceed itemsProcessed ($itemsProcessed)"
        }
    }

    /** True once every known target has been processed — the moment
     *  right before RunScanRequestUseCase persists the final ScanResult.
     *  Deliberately requires totalItems > 0 so the brief "nothing resolved
     *  yet" starting state never reads as already complete. */
    val isComplete: Boolean
        get() = totalItems > 0 && itemsProcessed == totalItems

    companion object {
        /** The state right after a session starts, before enumeration has
         *  resolved how many targets exist. */
        fun starting(sessionId: String): ScanProgress =
            ScanProgress(sessionId, itemsProcessed = 0, totalItems = 0, threatsFoundSoFar = 0)
    }
}
