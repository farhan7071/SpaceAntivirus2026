package com.space.antivirus.feature.security

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
 * Same proportionate testing choice as HomeViewModelTest (ADR 0030):
 * mockk on SecurityRepository, stubbed only for the one method actually
 * called, fed into a real ObserveScanHistoryUseCase — not a hand-written
 * local Fake covering all 14 SecurityRepository methods this ViewModel
 * never touches.
 */
class SecurityCenterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val securityRepository = mockk<SecurityRepository>()

    private fun buildViewModel(): SecurityCenterViewModel =
        SecurityCenterViewModel(ObserveScanHistoryUseCase(securityRepository))

    private fun completedSession(id: String = "s1") = ScanSession(
        id = id,
        scanType = ScanType.QUICK,
        state = ScanSessionState.COMPLETED,
        startedAtEpochMillis = 1_000L,
        completedAtEpochMillis = 2_000L,
    )

    private fun cleanScanResult() = ScanResult(
        session = completedSession(),
        statistics = ScanStatistics(
            itemsScanned = 10,
            threatsFound = 0,
            itemsInconclusive = 0,
            itemsTrusted = 0,
            durationMillis = 500,
        ),
        threats = emptyList(),
    )

    private fun threat(
        id: String,
        title: String = "Unusual permission combination",
        description: String = "test description $id",
        riskLevel: RiskLevel = RiskLevel.ATTENTION,
    ) = Threat(
        id = id,
        targetIdentifier = "com.example.app.$id",
        threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
        riskLevel = riskLevel,
        title = title,
        description = description,
        detections = listOf(
            Detection(
                id = "$id-d1",
                analyzerId = AnalyzerId("test"),
                threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                evidenceDescription = "test evidence",
                riskLevel = riskLevel,
            ),
        ),
        discoveredAtEpochMillis = 2_000L,
    )

    private fun flaggedScanResult(threats: List<Threat>) = ScanResult(
        session = completedSession(),
        statistics = ScanStatistics(
            itemsScanned = 10,
            threatsFound = threats.size,
            itemsInconclusive = 0,
            itemsTrusted = 0,
            durationMillis = 500,
        ),
        threats = threats,
    )

    @Test
    fun `no scan history yields UNKNOWN status and an empty threat list`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.UNKNOWN)
            assertThat(state.threats).isEmpty()
        }
    }

    @Test
    fun `a clean completed scan yields PROTECTED status and an empty threat list`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(cleanScanResult()))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.PROTECTED)
            assertThat(state.threats).isEmpty()
        }
    }

    @Test
    fun `a scan with threats yields NEEDS_ATTENTION and the full threat detail list`() = runTest {
        val threats = listOf(
            threat("t1", title = "Unusual permission combination", description = "SMS + Internet"),
            threat("t2", title = "Possible app impersonation", description = "Identity mismatch"),
        )
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(threats)))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.NEEDS_ATTENTION)
            assertThat(state.threats).hasSize(2)
            assertThat(state.threats.map { it.title }).containsExactly(
                "Unusual permission combination",
                "Possible app impersonation",
            )
        }
    }

    @Test
    fun `each ThreatSummary carries the real title, description, and riskLevel from the domain Threat`() =
        runTest {
            val realThreat = threat(
                "t1",
                title = "Unusual permission combination",
                description = "Requests SMS access together with INTERNET access",
                riskLevel = RiskLevel.ATTENTION,
            )
            every { securityRepository.observeScanHistory() } returns
                flowOf(listOf(flaggedScanResult(listOf(realThreat))))

            buildViewModel().uiState.test {
                assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
                val state = awaitItem() as SecurityCenterUiState.Loaded
                val summary = state.threats.single()
                assertThat(summary.title).isEqualTo("Unusual permission combination")
                assertThat(summary.description).isEqualTo("Requests SMS access together with INTERNET access")
                assertThat(summary.riskLevel).isEqualTo(RiskLevel.ATTENTION)
            }
        }

    @Test
    fun `the most recently completed scan is used, matching observeScanHistory's most-recent-first ordering`() =
        runTest {
            val mostRecent = flaggedScanResult(listOf(threat("t1")))
            val older = cleanScanResult()
            every { securityRepository.observeScanHistory() } returns flowOf(listOf(mostRecent, older))

            buildViewModel().uiState.test {
                assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
                val state = awaitItem() as SecurityCenterUiState.Loaded
                assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.NEEDS_ATTENTION)
                assertThat(state.threats).hasSize(1)
            }
        }

    @Test
    fun `an upstream failure surfaces as Error state, not a crash`() = runTest {
        every { securityRepository.observeScanHistory() } returns flow { throw IllegalStateException("db error") }

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem()
            assertThat(state).isInstanceOf(SecurityCenterUiState.Error::class.java)
        }
    }
}
