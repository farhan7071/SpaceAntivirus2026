package com.space.antivirus.core.enumeration.di

import com.space.antivirus.core.enumeration.EnumerationRepositoryImpl
import com.space.antivirus.domain.repository.EnumerationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the EnumerationRepository interface (domain) to its
 * EnumerationRepositoryImpl (this module). Uses @Binds on an abstract
 * class rather than a @Provides factory function in an object — the
 * idiomatic Hilt pattern for "here's the one implementation of this
 * interface" bindings, as opposed to @Provides which is for constructing
 * something Hilt can't build via @Inject alone (see NetworkModule,
 * DispatcherModule for examples of that other case).
 *
 * This is also the first interface-to-implementation binding in the
 * project — SecurityRepository (Sprint 004A) has no implementation yet,
 * so this sets the pattern that binding will eventually follow too.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EnumerationBindingModule {

    @Binds
    abstract fun bindEnumerationRepository(
        impl: EnumerationRepositoryImpl,
    ): EnumerationRepository
}
