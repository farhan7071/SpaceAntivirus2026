package com.space.antivirus.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.space.antivirus.core.database.AppDatabase
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
}
