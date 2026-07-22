package com.space.antivirus.core.model

/**
 * The combined "report" view of a completed scan — what a UseCase returns
 * to a ViewModel to render a results screen (in a later sprint). Composes
 * ScanSession (lifecycle), ScanStatistics (numbers), and the Threats found,
 * rather than duplicating any of their fields — single source of truth
 * for each concern.
 */
data class ScanResult(
    val session: ScanSession,
    val statistics: ScanStatistics,
    val threats: List<Threat>,
) {
    init {
        require(session.state in ScanSession.TERMINAL_STATES) {
            "A ScanResult can only be produced for a session in a terminal " +
                "state (was ${session.state}) — an in-progress session has no " +
                "final report yet."
        }
        require(statistics.threatsFound == threats.size) {
            "statistics.threatsFound (${statistics.threatsFound}) must match " +
                "the actual threats list size (${threats.size})"
        }
    }

    /** True when the scan completed cleanly with no findings — the
     *  positive, reassuring "no threats found" state (Sprint 002.5 §15 /
     *  Sprint 002.75 §10), not merely the absence of a result. */
    val isClean: Boolean
        get() = session.state == ScanSessionState.COMPLETED && threats.isEmpty()
}
