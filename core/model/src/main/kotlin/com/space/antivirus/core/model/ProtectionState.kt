package com.space.antivirus.core.model

/**
 * The complete, observable state of background protection — Sprint 042.
 *
 * One type rather than four separate Flows because every consumer
 * (Home's protection row, Settings, the boot receiver) needs the same
 * combination, and combining them independently in three places is how
 * they drift.
 *
 * `lastScheduledAtEpochMillis` is null until protection has been
 * successfully scheduled at least once, and stays set afterwards even
 * while disabled — it is a historical fact, not a live status. See
 * `BackgroundProtectionPreferences` for why it is only ever written
 * after a confirmed scheduler success.
 */
data class ProtectionState(
    val isEnabled: Boolean,
    val intervalHours: Long,
    val lastScheduledAtEpochMillis: Long?,
    val notifyAfterScan: Boolean,
) {
    init {
        require(intervalHours > 0) { "intervalHours must be positive, was $intervalHours" }
    }

    /**
     * The earliest the next scheduled scan could run, or null when
     * protection is off or has never been scheduled.
     *
     * Deliberately named "earliest" and documented as such: WorkManager
     * decides when periodic work actually fires, and defers it for the
     * battery and storage constraints this project sets. Presenting this
     * as an exact "next scan at 14:30" would be stating a guarantee the
     * platform does not make. Callers should render it as approximate.
     */
    val earliestNextScanEpochMillis: Long?
        get() = if (!isEnabled) null else lastScheduledAtEpochMillis?.plus(intervalHours * MILLIS_PER_HOUR)

    private companion object {
        const val MILLIS_PER_HOUR = 3_600_000L
    }
}
