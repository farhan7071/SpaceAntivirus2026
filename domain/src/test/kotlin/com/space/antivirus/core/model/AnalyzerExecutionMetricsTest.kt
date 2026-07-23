package com.space.antivirus.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnalyzerExecutionMetricsTest {

    @Test
    fun `constructs successfully with valid values`() {
        val metrics = AnalyzerExecutionMetrics(AnalyzerId("test"), durationMillis = 150L, succeeded = true)
        assertThat(metrics.durationMillis).isEqualTo(150L)
        assertThat(metrics.succeeded).isTrue()
    }

    @Test
    fun `rejects negative durationMillis`() {
        val exception = runCatching {
            AnalyzerExecutionMetrics(AnalyzerId("test"), durationMillis = -1L, succeeded = false)
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
