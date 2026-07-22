package com.space.antivirus.core.model

/**
 * The lifecycle aggregate for one scan, from creation through completion,
 * cancellation, or failure. This models ONLY the lifecycle — see
 * ScanResult for the combined "report" view (session + statistics +
 * threats) that a completed scan produces.
 *
 * Allowed state transitions (enforced by the repository implementation in
 * a later sprint, not by this data class — a data class can't enforce
 * temporal rules on its own):
 * PENDING -> RUNNING -> COMPLETED
 * PENDING -> RUNNING -> CANCELLED
 * PENDING -> RUNNING -> FAILED
 * PENDING -> CANCELLED (cancelled before it ever started running)
 */
data class ScanSession(
    val id: String,
    val scanType: ScanType,
    val state: ScanSessionState,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
) {
    init {
        val isTerminal = state in TERMINAL_STATES
        require(isTerminal == (completedAtEpochMillis != null)) {
            "completedAtEpochMillis must be set if and only if state is terminal " +
                "(was state=$state, completedAtEpochMillis=$completedAtEpochMillis)"
        }
        completedAtEpochMillis?.let {
            require(it >= startedAtEpochMillis) {
                "completedAtEpochMillis ($it) cannot precede startedAtEpochMillis ($startedAtEpochMillis)"
            }
        }
    }

    companion object {
        val TERMINAL_STATES = setOf(
            ScanSessionState.COMPLETED,
            ScanSessionState.CANCELLED,
            ScanSessionState.FAILED,
        )
    }
}
