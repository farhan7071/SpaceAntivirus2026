package com.space.antivirus.core.protection.di

import com.space.antivirus.core.protection.AndroidBatteryOptimizationStatus
import com.space.antivirus.core.protection.NotificationHelper
import com.space.antivirus.core.protection.ProtectionManagerImpl
import com.space.antivirus.domain.protection.BatteryOptimizationStatus
import com.space.antivirus.domain.protection.ProtectionManager
import com.space.antivirus.domain.protection.ProtectionNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Same @Binds pattern as every binding module since Sprint 004B
 *  (ADR 0014). All three are @Singleton: the notifier owns channel
 *  creation and the manager owns ordering, neither of which should be
 *  duplicated per injection site. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProtectionBindingModule {

    @Binds
    @Singleton
    abstract fun bindProtectionManager(impl: ProtectionManagerImpl): ProtectionManager

    @Binds
    @Singleton
    abstract fun bindProtectionNotifier(impl: NotificationHelper): ProtectionNotifier

    @Binds
    @Singleton
    abstract fun bindBatteryOptimizationStatus(
        impl: AndroidBatteryOptimizationStatus,
    ): BatteryOptimizationStatus
}
