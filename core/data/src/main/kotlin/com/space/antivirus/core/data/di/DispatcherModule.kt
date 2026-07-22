package com.space.antivirus.core.data.di

import com.space.antivirus.core.common.DefaultDispatcher
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.common.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * BUGFIX (Sprint 004A): core:common's @IoDispatcher/@DefaultDispatcher/
 * @MainDispatcher qualifiers were declared in Sprint 003 but never bound
 * to a real CoroutineDispatcher anywhere — they had no consumers until
 * this sprint's domain UseCase layer injected @IoDispatcher directly.
 * Without this module, Hilt's compile-time graph validation fails with
 * "cannot provide CoroutineDispatcher annotated with @IoDispatcher".
 * Lives here (not in core:common) because core:common is a pure-Kotlin
 * JVM module (ADR 0011) and can't host Hilt/Dagger processing itself;
 * core:data is already the established home for infrastructure @Module
 * bindings (see NetworkModule, DataModule in this same package).
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
