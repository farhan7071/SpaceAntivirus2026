package com.space.antivirus.core.testing

import kotlinx.coroutines.test.StandardTestDispatcher

/**
 * Test double for core:common's AppDispatchers — every Repository/UseCase
 * test injects this instead of the real Dispatchers.IO/Default so tests
 * run synchronously and deterministically.
 */
object TestDispatchers {
    val standard = StandardTestDispatcher()
}
