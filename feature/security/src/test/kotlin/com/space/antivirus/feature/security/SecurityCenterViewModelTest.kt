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
import com.space.antivirus.domain.reporting.ThreatDescriptionProvider
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
 *
 * Sprint 029: descriptionProvider is also mocked — needed for
 * recommendationFor(threatType), stubbed to a fixed value per test since
 * the ViewModel's own job is calling it, not what it returns (that's
 * ProductionThreatDescriptionProvider's own test's job).
 */
class SecurityCenterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val securityRepository = mockk<SecurityRepository>()
    private val descriptionProvider = mockk<ThreatDescriptionProvider>()

    private fun buildViewModel(): SecurityCenterViewModel {
        every { descriptionProvider.recommendationFor(any()) } returns "test recommendation"
        return SecurityCenterViewModel(ObserveScanHistoryUseCase(securityRepository), descriptionProvider)
    }

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
        appLabel: String = "Example App $id",
        packageName: String = "com.example.app.$id",
        evidenceDescription: String = "test evidence $id",
        riskLevel: RiskLevel = RiskLevel.ATTENTION,
    ) = Threat(
        id = id,
        targetIdentifier = packageName,
        threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
        riskLevel = riskLevel,
        title = "Unusual permission combination",
        description = "legacy description $id",
        detections = listOf(
            Detection(
                id = "$id-d1",
                analyzerId = AnalyzerId("test"),
                threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                evidenceDescription = evidenceDescription,
                riskLevel = riskLevel,
            ),
        ),
        discoveredAtEpochMillis = 2_000L,
        appLabel = appLabel,
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
    fun `a scan with threats yields NEEDS_ATTENTION and one ThreatSummary per app, identified by appLabel`() =
        runTest {
            val threats = listOf(
                threat("t1", appLabel = "Chrome"),
                threat("t2", appLabel = "WhatsApp"),
            )
            every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(threats)))

            buildViewModel().uiState.test {
                assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
                val state = awaitItem() as SecurityCenterUiState.Loaded
                assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.NEEDS_ATTENTION)
                assertThat(state.threats).hasSize(2)
                // Sprint 029 root-cause fix, verified at the ViewModel
                // layer: two DIFFERENT apps are distinguishable by name,
                // not indistinguishable behind the same generic title.
                assertThat(state.threats.map { it.appLabel }).containsExactly("Chrome", "WhatsApp")
            }
        }

    @Test
    fun `each ThreatSummary carries appLabel, packageName, riskLevel, reasons, and recommendation`() =
        runTest {
            every { descriptionProvider.recommendationFor(ThreatType.SUSPICIOUS_PERMISSION_USAGE) } returns
                "Review if unexpected."
            val realThreat = threat(
                "t1",
                appLabel = "Suspicious App",
                packageName = "com.example.suspicious",
                evidenceDescription = "SMS access with INTERNET access",
                riskLevel = RiskLevel.ATTENTION,
            )
            every { securityRepository.observeScanHistory() } returns
                flowOf(listOf(flaggedScanResult(listOf(realThreat))))

            buildViewModel().uiState.test {
                assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
                val state = awaitItem() as SecurityCenterUiState.Loaded
                val summary = state.threats.single()
                assertThat(summary.appLabel).isEqualTo("Suspicious App")
                assertThat(summary.packageName).isEqualTo("com.example.suspicious")
                assertThat(summary.riskLevel).isEqualTo(RiskLevel.ATTENTION)
                assertThat(summary.reasons).containsExactly("SMS access with INTERNET access")
                assertThat(summary.recommendation).isEqualTo("Review if unexpected.")
            }
        }

    @Test
    fun `a Threat with multiple Detections maps to multiple reasons, one bullet per Detection`() = runTest {
        val multiDetectionThreat = Threat(
            id = "t1",
            targetIdentifier = "com.example.app",
            threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            riskLevel = RiskLevel.ACTION_NEEDED,
            title = "Unusual permission combination",
            description = "legacy",
            detections = listOf(
                Detection(
                    id = "d1",
                    analyzerId = AnalyzerId("overlay-permission-pattern"),
                    threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                    evidenceDescription = "Overlay reason",
                    riskLevel = RiskLevel.ATTENTION,
                ),
                Detection(
                    id = "d2",
                    analyzerId = AnalyzerId("surveillance-permission-combination"),
                    threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                    evidenceDescription = "Surveillance reason",
                    riskLevel = RiskLevel.ATTENTION,
                ),
            ),
            discoveredAtEpochMillis = 2_000L,
            appLabel = "Example App",
        )
        every { securityRepository.observeScanHistory() } returns
            flowOf(listOf(flaggedScanResult(listOf(multiDetectionThreat))))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            val summary = state.threats.single()
            assertThat(summary.reasons).containsExactly("Overlay reason", "Surveillance reason")
        }
    }

    @Test
    fun `an empty appLabel falls back to the package name, defensively`() = runTest {
        val threatWithBlankLabel = threat("t1", appLabel = "", packageName = "com.example.blank")
        every { securityRepository.observeScanHistory() } returns
            flowOf(listOf(flaggedScanResult(listOf(threatWithBlankLabel))))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            assertThat(state.threats.single().appLabel).isEqualTo("com.example.blank")
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
