package com.space.antivirus.core.common

import javax.inject.Qualifier

/**
 * Injected instead of referencing Dispatchers.IO/Default directly, so
 * tests can supply a TestDispatcher without touching production code.
 * Bound to real Dispatchers.IO/Default/Main in core:data's
 * DispatcherModule (Sprint 004A) — these qualifiers were declared in
 * Sprint 003 with no actual @Provides binding and zero consumers; the
 * domain UseCase layer added in this sprint is the first real consumer,
 * which is what surfaced the gap.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
