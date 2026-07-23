package com.space.antivirus.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScanProgressTest {

    @Test
    fun `starting yields a valid, incomplete, zeroed snapshot`() {
        val progress = ScanProgress.starting("session-1")
        assertThat(progress.itemsProcessed).isEqualTo(0)
        assertThat(progress.totalItems).isEqualTo(0)
        assertThat(progress.isComplete).isFalse()
    }

    @Test
    fun `isComplete is true only once itemsProcessed reaches a known nonzero total`() {
        val complete = ScanProgress("s1", itemsProcessed = 5, totalItems = 5, threatsFoundSoFar = 0)
        assertThat(complete.isComplete).isTrue()
    }

    @Test
    fun `rejects itemsProcessed exceeding a nonzero totalItems`() {
        val exception = runCatching {
            ScanProgress("s1", itemsProcessed = 6, totalItems = 5, threatsFoundSoFar = 0)
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects threatsFoundSoFar exceeding itemsProcessed`() {
        val exception = runCatching {
            ScanProgress("s1", itemsProcessed = 2, totalItems = 10, threatsFoundSoFar = 3)
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects negative itemsProcessed`() {
        val exception = runCatching {
            ScanProgress("s1", itemsProcessed = -1, totalItems = 5, threatsFoundSoFar = 0)
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
