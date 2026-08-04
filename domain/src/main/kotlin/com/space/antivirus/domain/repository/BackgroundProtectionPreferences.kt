package com.space.antivirus.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Contract for the persisted user preferences behind the Settings
 * screen's background-protection controls (Sprint 026). Pure Kotlin, no
 * DataStore/Android type crosses this boundary — same discipline as
 * every prior repository in this project. The real implementation
 * (DataStoreBackgroundProtectionPreferences, core:data) wraps the
 * existing UserPreferencesDataSource rather than introducing a second
 * storage mechanism.
 *
 * Deliberately narrow and focused on this one feature's preferences,
 * not a general-purpose "SettingsRepository" — a broader contract would
 * invite scope creep into preferences this sprint has no reason to
 * touch (analytics, notifications), which already have their own
 * established, working access pattern in UserPreferencesDataSource.
 */
interface BackgroundProtectionPreferences {

    val isEnabled: Flow<Boolean>
    val intervalHours: Flow<Long>

    /** Null if background protection has never been successfully
     *  scheduled. Recorded only alongside a CONFIRMED successful
     *  scheduler call (see ProtectionManagerImpl) —
     *  this is what makes it an honest "last scheduled state" signal
     *  rather than an assumption; it can never silently drift from what
     *  actually happened. */
    val lastScheduledAtEpochMillis: Flow<Long?>

    /** Sprint 042. Whether to post a notification after each scheduled
     *  scan. Defaults to false: a security app that interrupts you every
     *  time it finds nothing is a security app you mute, and a muted app
     *  cannot tell you when something is actually wrong. */
    val notifyAfterScan: Flow<Boolean>

    /** Sets isEnabled=true, intervalHours, and lastScheduledAtEpochMillis
     *  together, atomically. Called only after a scheduler call has
     *  already succeeded — never called speculatively before knowing the
     *  real outcome. */
    suspend fun recordEnabled(intervalHours: Long, scheduledAtEpochMillis: Long)

    /** Sets isEnabled=false. Does not clear intervalHours or
     *  lastScheduledAtEpochMillis — a user re-enabling later should see
     *  their last-chosen interval still selected, and "last scheduled"
     *  remains a true historical fact even while currently disabled. */
    suspend fun recordDisabled()

    /** Persists an interval choice independent of enabled/disabled state
     *  — a user can select an interval while background protection is
     *  off; it takes effect the next time they turn it on. */
    suspend fun setIntervalHours(hours: Long)

    /** Sprint 042. Independent of enabled/disabled state, same as
     *  setIntervalHours. */
    suspend fun setNotifyAfterScan(enabled: Boolean)
}
