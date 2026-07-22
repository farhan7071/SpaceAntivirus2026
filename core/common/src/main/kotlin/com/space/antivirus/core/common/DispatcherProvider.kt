package com.space.antivirus.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Injected instead of referencing Dispatchers.IO/Default directly, so
 * core:testing can supply a TestDispatcher without touching production
 * code. Required for the Testing Foundation (Task 13) to work at all.
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

@Singleton
class AppDispatchers @Inject constructor() {
    val io: CoroutineDispatcher = Dispatchers.IO
    val default: CoroutineDispatcher = Dispatchers.Default
    val main: CoroutineDispatcher = Dispatchers.Main
}
