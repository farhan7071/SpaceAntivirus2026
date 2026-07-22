package com.space.antivirus.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
 */
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
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
}
