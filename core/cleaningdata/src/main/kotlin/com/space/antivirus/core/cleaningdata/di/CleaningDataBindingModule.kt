package com.space.antivirus.core.cleaningdata.di

import com.space.antivirus.core.cleaningdata.CleanupHistoryRepositoryImpl
import com.space.antivirus.core.cleaningdata.FileDeletionRepositoryImpl
import com.space.antivirus.core.cleaningdata.StorageStatisticsRepositoryImpl
import com.space.antivirus.domain.repository.CleanupHistoryRepository
import com.space.antivirus.domain.repository.FileDeletionRepository
import com.space.antivirus.domain.repository.StorageStatisticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Same @Binds pattern as SecurityDataBindingModule (Sprint 011),
 *  TrustedDataBindingModule (Sprint 012) and EnumerationBindingModule
 *  (Sprint 004B, ADR 0014). */
@Module
@InstallIn(SingletonComponent::class)
abstract class CleaningDataBindingModule {

    @Binds
    abstract fun bindFileDeletionRepository(impl: FileDeletionRepositoryImpl): FileDeletionRepository

    @Binds
    abstract fun bindCleanupHistoryRepository(impl: CleanupHistoryRepositoryImpl): CleanupHistoryRepository

    @Binds
    abstract fun bindStorageStatisticsRepository(
        impl: StorageStatisticsRepositoryImpl,
    ): StorageStatisticsRepository
}
