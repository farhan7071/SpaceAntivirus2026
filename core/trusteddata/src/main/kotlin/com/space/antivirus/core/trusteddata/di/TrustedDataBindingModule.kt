package com.space.antivirus.core.trusteddata.di

import com.space.antivirus.core.trusteddata.TrustedItemRepositoryImpl
import com.space.antivirus.domain.repository.TrustedItemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Same @Binds pattern as SecurityDataBindingModule (Sprint 011) and
 *  EnumerationBindingModule (Sprint 004B, ADR 0014). */
@Module
@InstallIn(SingletonComponent::class)
abstract class TrustedDataBindingModule {

    @Binds
    abstract fun bindTrustedItemRepository(impl: TrustedItemRepositoryImpl): TrustedItemRepository
}
