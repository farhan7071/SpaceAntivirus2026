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

class UnknownInstallerSourceAnalyzerTest {

    private val analyzer = UnknownInstallerSourceAnalyzer()

    private fun appTarget(installerPackageName: String?, isSystemApp: Boolean = false) =
        ScanTarget.ApplicationTarget(
            InstalledApplicationInfo(
                packageName = "com.example.app",
                appLabel = "Example",
                versionName = "1.0",
                versionCode = 1L,
                installedAtEpochMillis = 0L,
                isSystemApp = isSystemApp,
                apkPath = "/data/app/example.apk",
                requestedPermissions = emptyList(),
                installerPackageName = installerPackageName,
            ),
        )

    @Test
    fun `a null installer is Flagged`() = runTest {
        val result = analyzer.analyze(appTarget(installerPackageName = null))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }

    @Test
    fun `flagged at INFO severity and LOW confidence - the most conservative analyzer in this project`() = runTest {
        val result = analyzer.analyze(appTarget(installerPackageName = null))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        val detection = outcome.detections.single()
        assertThat(detection.riskLevel).isEqualTo(RiskLevel.INFO)
        assertThat(detection.confidence).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `a known installer (e-g- the Play Store) is Clean`() = runTest {
        val result = analyzer.analyze(appTarget(installerPackageName = "com.android.vending"))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a system app with a null installer is still Clean`() = runTest {
        val result = analyzer.analyze(appTarget(installerPackageName = null, isSystemApp = true))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }
}
