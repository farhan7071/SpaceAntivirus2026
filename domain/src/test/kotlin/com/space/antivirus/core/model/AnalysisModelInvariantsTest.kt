package com.space.antivirus.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnalysisModelInvariantsTest {

    @Test
    fun `AnalyzerId rejects blank values`() {
        val exception = runCatching { AnalyzerId("") }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `AnalyzerId accepts a non-blank value`() {
        val id = AnalyzerId("signature-engine-v1")
        assertThat(id.value).isEqualTo("signature-engine-v1")
    }

    @Test
    fun `AnalysisOutcome Flagged requires at least one Detection`() {
        val exception = runCatching {
            AnalysisOutcome.Flagged(targetIdentifier = "com.example.app", detections = emptyList())
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `AnalysisOutcome Inconclusive rejects a blank reason`() {
        val exception = runCatching {
            AnalysisOutcome.Inconclusive(targetIdentifier = "file.txt", reason = "")
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `AnalysisOutcome Clean requires no additional data`() {
        val outcome = AnalysisOutcome.Clean(targetIdentifier = "com.example.app")
        assertThat(outcome.targetIdentifier).isEqualTo("com.example.app")
    }
}
