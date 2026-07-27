package com.space.antivirus.core.analysisengine.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanTarget
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeviceAdministratorAnalyzerTest {

    private val analyzer = DeviceAdministratorAnalyzer()

    private fun appTarget(permissions: List<String> = emptyList(), isSystemApp: Boolean = false) =
        ScanTarget.ApplicationTarget(
            InstalledApplicationInfo(
                packageName = "com.example.app",
                appLabel = "Example",
                versionName = "1.0",
                versionCode = 1L,
                installedAtEpochMillis = 0L,
                isSystemApp = isSystemApp,
                apkPath = "/data/app/example.apk",
                requestedPermissions = permissions,
            ),
        )

    @Test
    fun `device admin alone (no internet needed) is Flagged`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = listOf("android.permission.BIND_DEVICE_ADMIN")))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }

    @Test
    fun `flagged at INFO severity, not ATTENTION - standalone capability, not a combo`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = listOf("android.permission.BIND_DEVICE_ADMIN")))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().riskLevel).isEqualTo(RiskLevel.INFO)
    }

    @Test
    fun `no device admin permission is Clean`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = emptyList()))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a system app with device admin is still Clean`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.BIND_DEVICE_ADMIN"), isSystemApp = true),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `confidence is HIGH - the permission's presence is a hard fact, not an inference`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = listOf("android.permission.BIND_DEVICE_ADMIN")))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.HIGH)
    }

    @Test
    fun `device admin plus internet is Clean here - Sprint 028 fix, avoids overlap with the existing combo rule`() =
        runTest {
            // SuspiciousPermissionPatternAnalyzer's device-admin+INTERNET
            // combo rule already covers this exact app, more specifically
            // and at higher severity — this analyzer firing too would be a
            // second, redundant finding about the same underlying fact.
            val result = analyzer.analyze(
                appTarget(
                    permissions = listOf("android.permission.BIND_DEVICE_ADMIN", "android.permission.INTERNET"),
                ),
            )

            assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
        }

    @Test
    fun `device admin without internet is still Flagged - this analyzer's own reason to exist`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = listOf("android.permission.BIND_DEVICE_ADMIN")))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }
}
