package com.space.antivirus.core.securitydata.di

import com.space.antivirus.core.securitydata.SecurityRepositoryImpl
import com.space.antivirus.domain.repository.SecurityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Same pattern as EnumerationBindingModule (Sprint 004B, ADR 0014) — the
 * first repository to actually follow that pattern besides enumeration.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityDataBindingModule {

    @Binds
    abstract fun bindSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository
}
