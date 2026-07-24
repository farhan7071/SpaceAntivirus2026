package com.space.antivirus.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.space.antivirus.core.database.AppDatabase
import com.space.antivirus.core.database.dao.DetectionDao
import com.space.antivirus.core.database.dao.ScanSessionDao
import com.space.antivirus.core.database.dao.ScanStatisticsDao
import com.space.antivirus.core.database.dao.ThreatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATASTORE_NAME = "space_antivirus_prefs"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "space_antivirus.db")
            // Sprint 010 bumped the schema version 1 -> 2 (real entities
            // replacing Sprint 003's PlaceholderEntity — see ADR 0023).
            // No real Migration object exists yet because this database
            // has never shipped with actual persisted rows; destructive
            // migration is the correct, honest choice for a pre-1.0 app
            // at this stage, not a shortcut around writing one. Revisit
            // this the moment real user data needs to survive a schema
            // change.
            .fallbackToDestructiveMigration()
            .build()

    // Sprint 011: DAOs are provided here (not re-declared in
    // core:securitydata) so SecurityRepositoryImpl can @Inject them
    // directly without needing to know how AppDatabase itself is
    // constructed — the same separation FakeSecurityRepository's tests
    // already enjoy, just for the real implementation.

    @Provides
    @Singleton
    fun provideScanSessionDao(appDatabase: AppDatabase): ScanSessionDao = appDatabase.scanSessionDao()

    @Provides
    @Singleton
    fun provideScanStatisticsDao(appDatabase: AppDatabase): ScanStatisticsDao = appDatabase.scanStatisticsDao()

    @Provides
    @Singleton
    fun provideThreatDao(appDatabase: AppDatabase): ThreatDao = appDatabase.threatDao()

    @Provides
    @Singleton
    fun provideDetectionDao(appDatabase: AppDatabase): DetectionDao = appDatabase.detectionDao()
}
