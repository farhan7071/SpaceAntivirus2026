package com.space.antivirus.core.model

/** Lifecycle states a ScanSession can be in. A session moves strictly
 *  forward through this lifecycle — see ScanSession's KDoc for the
 *  allowed transitions, enforced by the repository contract, not by this
 *  enum itself (an enum can't enforce transition rules on its own). */
enum class ScanSessionState {
    PENDING,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED,
}
