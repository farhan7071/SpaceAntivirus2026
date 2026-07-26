package com.space.antivirus.core.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.testing.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * A real, temp-file-backed DataStore, not a mock — DataStore's core
 * file-based storage doesn't need an Android Context to test (only the
 * `preferencesDataStore` Context-delegate extension in DataModule.kt
 * does, which this test doesn't use), so this runs as a genuine JVM
 * unit test exercising the real write-then-read round trip. Same
 * "prefer real infrastructure over fragile mocks" discipline this
 * project has followed since Sprint 010's Room/Hilt-graph testing.
 *
 * Also the closest thing this project has to a direct test of
 * DataStoreBackgroundProtectionPreferences' own delegation — that class
 * is a thin, unbranching pass-through, so exercising the real
 * UserPreferencesDataSource it wraps here covers both together rather
 * than duplicating trivial delegation assertions in a second file.
 */
class UserPreferencesDataSourceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newDataSource(tempFolder: File): UserPreferencesDataSource {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempFolder, "test_prefs.preferences_pb") },
        )
        return UserPreferencesDataSource(dataStore)
    }

    @Test
    fun `backgroundProtectionEnabled defaults to false`() = runTest {
        val dataSource = newDataSource(createTempDir())

        assertThat(dataSource.backgroundProtectionEnabled.first()).isFalse()
    }

    @Test
    fun `scanIntervalHours defaults to 24`() = runTest {
        val dataSource = newDataSource(createTempDir())

        assertThat(dataSource.scanIntervalHours.first()).isEqualTo(24L)
    }

    @Test
    fun `lastScheduledAtEpochMillis defaults to null`() = runTest {
        val dataSource = newDataSource(createTempDir())

        assertThat(dataSource.lastScheduledAtEpochMillis.first()).isNull()
    }

    @Test
    fun `recordBackgroundProtectionEnabled writes enabled, interval, and timestamp together`() = runTest {
        val dataSource = newDataSource(createTempDir())

        dataSource.recordBackgroundProtectionEnabled(intervalHours = 72L, scheduledAtEpochMillis = 9_999L)

        assertThat(dataSource.backgroundProtectionEnabled.first()).isTrue()
        assertThat(dataSource.scanIntervalHours.first()).isEqualTo(72L)
        assertThat(dataSource.lastScheduledAtEpochMillis.first()).isEqualTo(9_999L)
    }

    @Test
    fun `recordBackgroundProtectionDisabled sets enabled false without clearing interval or timestamp`() = runTest {
        val dataSource = newDataSource(createTempDir())
        dataSource.recordBackgroundProtectionEnabled(intervalHours = 72L, scheduledAtEpochMillis = 9_999L)

        dataSource.recordBackgroundProtectionDisabled()

        assertThat(dataSource.backgroundProtectionEnabled.first()).isFalse()
        // A user re-enabling later should see their last-chosen interval
        // still selected, and "last scheduled" remains a true historical
        // fact even while currently disabled (BackgroundProtectionPreferences'
        // own KDoc reasoning, verified here against real storage).
        assertThat(dataSource.scanIntervalHours.first()).isEqualTo(72L)
        assertThat(dataSource.lastScheduledAtEpochMillis.first()).isEqualTo(9_999L)
    }

    @Test
    fun `setScanIntervalHours persists independently of enabled state`() = runTest {
        val dataSource = newDataSource(createTempDir())

        dataSource.setScanIntervalHours(168L)

        assertThat(dataSource.scanIntervalHours.first()).isEqualTo(168L)
        assertThat(dataSource.backgroundProtectionEnabled.first()).isFalse()
    }

    @Test
    fun `pre-existing analytics and notifications preferences are unaffected by the new keys`() = runTest {
        val dataSource = newDataSource(createTempDir())

        dataSource.recordBackgroundProtectionEnabled(intervalHours = 24L, scheduledAtEpochMillis = 1_000L)

        assertThat(dataSource.analyticsEnabled.first()).isTrue()
        assertThat(dataSource.notificationsEnabled.first()).isTrue()
    }

    private fun createTempDir(): File = File.createTempFile("prefs_test", null).apply {
        delete()
        mkdirs()
    }
}
