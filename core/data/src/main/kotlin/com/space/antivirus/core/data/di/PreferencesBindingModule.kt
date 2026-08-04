package com.space.antivirus.core.data.di

import com.space.antivirus.core.data.AndroidAppInfoProvider
import com.space.antivirus.core.data.preferences.DataStoreBackgroundProtectionPreferences
import com.space.antivirus.domain.repository.BackgroundProtectionPreferences
import com.space.antivirus.domain.support.AppInfoProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** First domain-contract binding to live in core:data — every prior
 *  binding module here (none existed before Sprint 026) only provided
 *  raw infrastructure (DataStore, AppDatabase). Same @Binds pattern as
 *  every other repository-implementation module in this project. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesBindingModule {

    /** Sprint 043A. Lives here because core:data already owns the
     *  Context-bound platform readers this module binds. */
    @Binds
    abstract fun bindAppInfoProvider(impl: AndroidAppInfoProvider): AppInfoProvider

    @Binds
    abstract fun bindBackgroundProtectionPreferences(
        impl: DataStoreBackgroundProtectionPreferences,
    ): BackgroundProtectionPreferences
}
