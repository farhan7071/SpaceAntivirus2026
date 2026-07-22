package com.space.antivirus.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Foundation-level sanity test — real feature tests come with each
 * feature module (out of scope for Sprint 003 per the "no business logic"
 * rule), but the Result wrapper itself is foundation and gets tested here.
 */
class AppResultTest {

    @Test
    fun `map transforms success value`() {
        val result: AppResult<Int> = AppResult.Success(2)
        val mapped = result.map { it * 2 }
        assertEquals(AppResult.Success(4), mapped)
    }

    @Test
    fun `map leaves failure untouched`() {
        val result: AppResult<Int> = AppResult.Failure(AppError.Network)
        val mapped = result.map { it * 2 }
        assertEquals(AppResult.Failure(AppError.Network), mapped)
    }
}
