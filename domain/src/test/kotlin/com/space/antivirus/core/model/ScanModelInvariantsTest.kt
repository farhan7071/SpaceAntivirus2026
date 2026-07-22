package com.space.antivirus.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * These invariants (Threat needs evidence, statistics can't be negative
 * or internally inconsistent, ScanResult's threat count must match its
 * list) are the domain's actual behavior, not just data holders — worth
 * testing directly, independent of any UseCase.
 */
class ScanModelInvariantsTest {

    @Test
    fun `Threat requires at least one Detection`() {
        val exception = runCatching {
            Threat(
                id = "t1",
                targetIdentifier = "com.example.app",
                threatType = ThreatType.UNKNOWN,
                riskLevel = RiskLevel.INFO,
                title = "title",
                description = "description",
                detections = emptyList(),
                discoveredAtEpochMillis = 0L,
            )
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `ScanStatistics rejects threatsFound greater than itemsScanned`() {
        val exception = runCatching {
            ScanStatistics(itemsScanned = 5, threatsFound = 10, durationMillis = 100)
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `ScanStatistics EMPTY is valid and zeroed`() {
        assertThat(ScanStatistics.EMPTY.itemsScanned).isEqualTo(0)
        assertThat(ScanStatistics.EMPTY.threatsFound).isEqualTo(0)
    }

    @Test
    fun `ScanSession requires completedAt for terminal states`() {
        val exception = runCatching {
            ScanSession(
                id = "s1",
                scanType = ScanType.QUICK,
                state = ScanSessionState.COMPLETED,
                startedAtEpochMillis = 0L,
                completedAtEpochMillis = null,
            )
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `ScanSession rejects completedAt before startedAt`() {
        val exception = runCatching {
            ScanSession(
                id = "s1",
                scanType = ScanType.QUICK,
                state = ScanSessionState.COMPLETED,
                startedAtEpochMillis = 1000L,
                completedAtEpochMillis = 500L,
            )
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `ScanResult rejects mismatched threatsFound and threats list size`() {
        val session = ScanSession(
            id = "s1",
            scanType = ScanType.QUICK,
            state = ScanSessionState.COMPLETED,
            startedAtEpochMillis = 0L,
            completedAtEpochMillis = 100L,
        )
        val exception = runCatching {
            ScanResult(
                session = session,
                statistics = ScanStatistics(itemsScanned = 10, threatsFound = 2, durationMillis = 100),
                threats = emptyList(),
            )
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `ScanResult isClean is true only for completed sessions with no threats`() {
        val session = ScanSession(
            id = "s1",
            scanType = ScanType.QUICK,
            state = ScanSessionState.COMPLETED,
            startedAtEpochMillis = 0L,
            completedAtEpochMillis = 100L,
        )
        val result = ScanResult(
            session = session,
            statistics = ScanStatistics(itemsScanned = 10, threatsFound = 0, durationMillis = 100),
            threats = emptyList(),
        )
        assertThat(result.isClean).isTrue()
    }

    @Test
    fun `ScanResult rejects a non-terminal session`() {
        val session = ScanSession(
            id = "s1",
            scanType = ScanType.QUICK,
            state = ScanSessionState.RUNNING,
            startedAtEpochMillis = 0L,
        )
        val exception = runCatching {
            ScanResult(session, ScanStatistics.EMPTY, emptyList())
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
