package com.space.antivirus.core.ads.di

import com.space.antivirus.core.ads.AdTimeSource
import com.space.antivirus.core.ads.AdsController
import com.space.antivirus.core.ads.ConsentProvider
import com.space.antivirus.core.ads.GoogleAdsController
import com.space.antivirus.core.ads.SystemAdTimeSource
import com.space.antivirus.core.ads.UmpConsentManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Sprint 044. Same @Binds pattern as every binding module since Sprint
 * 004B (ADR 0014).
 *
 * `GoogleAdsController` is bound unconditionally rather than swapping in
 * `NoOpAdsController` for debug builds. Deliberate: the debug check
 * lives inside the controller and reads the installed package's real
 * debuggable flag, so behaviour is identical whether a debug build was
 * assembled locally or handed to a tester. A variant-based binding would
 * mean the ads path was never exercised in development at all, and the
 * first time it ran would be in production.
 *
 * `NoOpAdsController` stays available for tests and for any future build
 * flavour that ships without ads.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {

    @Binds
    @Singleton
    abstract fun bindAdsController(impl: GoogleAdsController): AdsController

    @Binds
    @Singleton
    abstract fun bindAdTimeSource(impl: SystemAdTimeSource): AdTimeSource

    /** Sprint 049 swapped this one binding, exactly as Sprint 044 said
     *  it would. Nothing else in the module changed. */
    @Binds
    @Singleton
    abstract fun bindConsentProvider(impl: UmpConsentManager): ConsentProvider
}
