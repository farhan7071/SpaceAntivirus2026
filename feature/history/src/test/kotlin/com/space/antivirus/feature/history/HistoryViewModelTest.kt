package com.space.antivirus.feature.history

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.core.model.Threat
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.core.testing.MainDispatcherRule
import com.space.antivirus.domain.repository.SecurityRepository
import com.space.antivirus.domain.usecase.ObserveScanHistoryUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Same proportionate testing choice as every prior ViewModel test in
 * this project: mockk on SecurityRepository, stubbed only for the one
 * method actually called, fed into a real ObserveScanHistoryUseCase.
 */
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val securityRepository = mockk<SecurityRepository>()

    private fun buildViewModel(): HistoryViewModel =
        HistoryViewModel(ObserveScanHistoryUseCase(securityRepository))

    private fun completedSession(id: String, completedAt: Long) = ScanSession(
        id = id,
        scanType = ScanType.QUICK,
        state = ScanSessionState.COMPLETED,
        startedAtEpochMillis = completedAt - 500,
        completedAtEpochMillis = completedAt,
    )

    private fun cleanScanResult(id: String, completedAt: Long, itemsScanned: Int = 10) = ScanResult(
        session = completedSession(id, completedAt),
        statistics = ScanStatistics(
            itemsScanned = itemsScanned,
            threatsFound = 0,
            itemsInconclusive = 0,
            itemsTrusted = 0,
            durationMillis = 500,
        ),
        threats = emptyList(),
    )

    private fun flaggedScanResult(id: String, completedAt: Long, threatCount: Int) = ScanResult(
        session = completedSession(id, completedAt),
        statistics = ScanStatistics(
            itemsScanned = 10,
            threatsFound = threatCount,
            itemsInconclusive = 0,
            itemsTrusted = 0,
            durationMillis = 750,
        ),
        threats = List(threatCount) { index ->
            Threat(
                id = "$id-t$index",
                targetIdentifier = "com.example.app$index",
                threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                riskLevel = RiskLevel.ATTENTION,
                title = "Unusual permission combination",
                description = "test description $index",
                detections = listOf(
                    Detection(
                        id = "$id-d$index",
                        analyzerId = AnalyzerId("test"),
                        threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                        evidenceDescription = "test evidence",
                        riskLevel = RiskLevel.ATTENTION,
                    ),
                ),
                discoveredAtEpochMillis = completedAt,
            )
        },
    )

    @Test
    fun `no scan history yields an empty entries list`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HistoryUiState.Loading)
            val state = awaitItem() as HistoryUiState.Loaded
            assertThat(state.entries).isEmpty()
        }
    }

    @Test
    fun `every completed scan is mapped to an entry, in the same order the repository returns them`() = runTest {
        val results = listOf(
            flaggedScanResult("s2", completedAt = 2_000L, threatCount = 1),
            cleanScanResult("s1", completedAt = 1_000L),
        )
        every { securityRepository.observeScanHistory() } returns flowOf(results)

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HistoryUiState.Loading)
            val state = awaitItem() as HistoryUiState.Loaded
            assertThat(state.entries).hasSize(2)
            assertThat(state.entries.map { it.sessionId }).containsExactly("s2", "s1").inOrder()
        }
    }

    @Test
    fun `a clean scan entry carries no threats and isClean true`() = runTest {
        every { securityRepository.observeScanHistory() } returns
            flowOf(listOf(cleanScanResult("s1", completedAt = 1_000L, itemsScanned = 15)))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HistoryUiState.Loading)
            val entry = (awaitItem() as HistoryUiState.Loaded).entries.single()
            assertThat(entry.isClean).isTrue()
            assertThat(entry.threats).isEmpty()
            assertThat(entry.itemsScanned).isEqualTo(15)
        }
    }

    @Test
    fun `a flagged scan entry carries the real threat titles, descriptions, and risk levels`() = runTest {
        every { securityRepository.observeScanHistory() } returns
            flowOf(listOf(flaggedScanResult("s1", completedAt = 1_000L, threatCount = 2)))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HistoryUiState.Loading)
            val entry = (awaitItem() as HistoryUiState.Loaded).entries.single()
            assertThat(entry.isClean).isFalse()
            assertThat(entry.threats).hasSize(2)
            assertThat(entry.threats.first().title).isEqualTo("Unusual permission combination")
            assertThat(entry.threats.first().riskLevel).isEqualTo(RiskLevel.ATTENTION)
        }
    }

    @Test
    fun `scan metadata - duration and itemsScanned - is carried through from ScanStatistics`() = runTest {
        every { securityRepository.observeScanHistory() } returns
            flowOf(listOf(cleanScanResult("s1", completedAt = 1_000L, itemsScanned = 42)))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HistoryUiState.Loading)
            val entry = (awaitItem() as HistoryUiState.Loaded).entries.single()
            assertThat(entry.durationMillis).isEqualTo(500)
            assertThat(entry.itemsScanned).isEqualTo(42)
        }
    }

    @Test
    fun `an upstream failure surfaces as Error state, not a crash`() = runTest {
        every { securityRepository.observeScanHistory() } returns flow { throw IllegalStateException("db error") }

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HistoryUiState.Loading)
            val state = awaitItem()
            assertThat(state).isInstanceOf(HistoryUiState.Error::class.java)
        }
    }
}
