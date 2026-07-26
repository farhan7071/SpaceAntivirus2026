package com.space.antivirus.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Routine (non-sensitive) settings live in DataStore, not
 * core:security's EncryptedSharedPreferences — see
 * docs/adr/0008-secure-preferences-scope.md for the split rationale.
 * Analytics opt-out is included here specifically because Sprint 002.75
 * §21 flagged that the Privacy copy promises this control must exist
 * before it ships — this is that control's foundation.
 *
 * Sprint 026: background-protection keys added. This class remains a
 * plain, low-level DataStore wrapper — it does not itself implement any
 * domain contract (that's DataStoreBackgroundProtectionPreferences,
 * which wraps this class), since a general-purpose preferences wrapper
 * may end up backing more than one focused domain contract over time.
 */
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BACKGROUND_PROTECTION_ENABLED = booleanPreferencesKey("background_protection_enabled")
        val SCAN_INTERVAL_HOURS = longPreferencesKey("scan_interval_hours")
        val LAST_SCHEDULED_AT_EPOCH_MILLIS = longPreferencesKey("last_scheduled_at_epoch_millis")
    }

    val analyticsEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.ANALYTICS_ENABLED] ?: true }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ANALYTICS_ENABLED] = enabled }
    }

    val notificationsEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    /** Defaults to false — background protection is opt-in, not silently
     *  on for every install. Same reasoning as ADR 0037's "foundation,
     *  not activation" — now that this preference genuinely controls
     *  activation, defaulting it off is what makes that reasoning still
     *  true rather than quietly reversed. */
    val backgroundProtectionEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.BACKGROUND_PROTECTION_ENABLED] ?: false }

    val scanIntervalHours: Flow<Long> =
        dataStore.data.map { it[Keys.SCAN_INTERVAL_HOURS] ?: DEFAULT_SCAN_INTERVAL_HOURS }

    val lastScheduledAtEpochMillis: Flow<Long?> =
        dataStore.data.map { it[Keys.LAST_SCHEDULED_AT_EPOCH_MILLIS] }

    /** Sets enabled, interval, and the scheduled-at timestamp in a single
     *  DataStore.edit{} transaction — these three always change together
     *  when background protection is successfully turned on, and writing
     *  them atomically avoids any window where a reader could observe an
     *  inconsistent partial state (e.g. enabled=true but no timestamp
     *  yet). */
    suspend fun recordBackgroundProtectionEnabled(intervalHours: Long, scheduledAtEpochMillis: Long) {
        dataStore.edit {
            it[Keys.BACKGROUND_PROTECTION_ENABLED] = true
            it[Keys.SCAN_INTERVAL_HOURS] = intervalHours
            it[Keys.LAST_SCHEDULED_AT_EPOCH_MILLIS] = scheduledAtEpochMillis
        }
    }

    suspend fun recordBackgroundProtectionDisabled() {
        dataStore.edit { it[Keys.BACKGROUND_PROTECTION_ENABLED] = false }
    }

    suspend fun setScanIntervalHours(hours: Long) {
        dataStore.edit { it[Keys.SCAN_INTERVAL_HOURS] = hours }
    }

    private companion object {
        const val DEFAULT_SCAN_INTERVAL_HOURS = 24L
    }
}
