package com.space.antivirus.core.data.preferences

import com.space.antivirus.domain.repository.BackgroundProtectionPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Real BackgroundProtectionPreferences implementation — a thin adapter
 * over the existing UserPreferencesDataSource, not a second storage
 * mechanism. Matches the same "contract in domain, real implementation
 * in an Android-capable module" pattern every prior repository in this
 * project has followed since Sprint 004B; the wrinkle here is that the
 * real implementation reuses an existing low-level data source rather
 * than owning storage directly, since UserPreferencesDataSource already
 * existed and already does exactly what's needed.
 */
class DataStoreBackgroundProtectionPreferences @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : BackgroundProtectionPreferences {

    override val isEnabled: Flow<Boolean> = dataSource.backgroundProtectionEnabled
    override val intervalHours: Flow<Long> = dataSource.scanIntervalHours
    override val lastScheduledAtEpochMillis: Flow<Long?> = dataSource.lastScheduledAtEpochMillis
    override val notifyAfterScan: Flow<Boolean> = dataSource.notifyAfterScan

    override suspend fun recordEnabled(intervalHours: Long, scheduledAtEpochMillis: Long) {
        dataSource.recordBackgroundProtectionEnabled(intervalHours, scheduledAtEpochMillis)
    }

    override suspend fun recordDisabled() {
        dataSource.recordBackgroundProtectionDisabled()
    }

    override suspend fun setIntervalHours(hours: Long) {
        dataSource.setScanIntervalHours(hours)
    }

    override suspend fun setNotifyAfterScan(enabled: Boolean) {
        dataSource.setNotifyAfterScan(enabled)
    }
}
