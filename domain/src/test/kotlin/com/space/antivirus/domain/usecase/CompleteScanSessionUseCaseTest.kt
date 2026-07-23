package com.space.antivirus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.core.model.Threat
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.fake.FakeSecurityRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CompleteScanSessionUseCaseTest {

    @Test
    fun `completing a session that was never started fails with ScanSessionNotFound`() = runTest {
        val repository = FakeSecurityRepository()
        val useCase = CompleteScanSessionUseCase(repository, StandardTestDispatcher(testScheduler))

        val result = useCase(
            CompleteScanSessionParams(
                sessionId = "does-not-exist",
                statistics = ScanStatistics.EMPTY,
                threats = emptyList(),
            ),
        )

        assertThat(result).isEqualTo(AppResult.Failure(AppError.ScanSessionNotFound("does-not-exist")))
    }

    @Test
    fun `completing a started session returns a clean ScanResult when no threats found`() = runTest {
        val repository = FakeSecurityRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val session = (
            StartScanSessionUseCase(repository, dispatcher)(ScanType.QUICK) as AppResult.Success
            ).data

        val result = CompleteScanSessionUseCase(repository, dispatcher)(
            CompleteScanSessionParams(
                sessionId = session.id,
                statistics = ScanStatistics(itemsScanned = 500, threatsFound = 0, durationMillis = 1200),
                threats = emptyList(),
            ),
        )

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        val scanResult = (result as AppResult.Success).data
        assertThat(scanResult.isClean).isTrue()
    }

    @Test
    fun `completing a started session with findings returns them attached`() = runTest {
        val repository = FakeSecurityRepository()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val session = (
            StartScanSessionUseCase(repository, dispatcher)(ScanType.FULL) as AppResult.Success
            ).data
        val threat = Threat(
            id = "threat-1",
            targetIdentifier = "com.example.suspicious",
            threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            riskLevel = RiskLevel.ATTENTION,
            title = "Unusual permission request",
            description = "This app requests SMS access unrelated to its stated purpose.",
            detections = listOf(
                Detection(
                    id = "d1",
                    analyzerId = AnalyzerId("permission-heuristic-test"),
                    threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                    evidenceDescription = "Requests READ_SMS without a messaging feature.",
                    riskLevel = RiskLevel.ATTENTION,
                ),
            ),
            discoveredAtEpochMillis = 0L,
        )

        val result = CompleteScanSessionUseCase(repository, dispatcher)(
            CompleteScanSessionParams(
                sessionId = session.id,
                statistics = ScanStatistics(itemsScanned = 200, threatsFound = 1, durationMillis = 800),
                threats = listOf(threat),
            ),
        )

        val scanResult = (result as AppResult.Success).data
        assertThat(scanResult.isClean).isFalse()
        assertThat(scanResult.threats).containsExactly(threat)
    }
}
