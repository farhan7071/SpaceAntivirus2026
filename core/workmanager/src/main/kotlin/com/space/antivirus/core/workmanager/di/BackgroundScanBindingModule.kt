package com.space.antivirus.core.workmanager.di

import com.space.antivirus.core.workmanager.WorkManagerBackgroundScanScheduler
import com.space.antivirus.domain.repository.BackgroundScanScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Same @Binds pattern as every prior repository-implementation module
 *  (EnumerationBindingModule, SecurityDataBindingModule, etc., since
 *  Sprint 004B). */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackgroundScanBindingModule {

    @Binds
    abstract fun bindBackgroundScanScheduler(impl: WorkManagerBackgroundScanScheduler): BackgroundScanScheduler
}
