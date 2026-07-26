package com.space.antivirus.core.analysisengine.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanTarget
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DebuggableApplicationAnalyzerTest {

    private val analyzer = DebuggableApplicationAnalyzer()

    private fun appTarget(isDebuggable: Boolean, isSystemApp: Boolean = false) = ScanTarget.ApplicationTarget(
        InstalledApplicationInfo(
            packageName = "com.example.app",
            appLabel = "Example",
            versionName = "1.0",
            versionCode = 1L,
            installedAtEpochMillis = 0L,
            isSystemApp = isSystemApp,
            apkPath = "/data/app/example.apk",
            requestedPermissions = emptyList(),
            isDebuggable = isDebuggable,
        ),
    )

    @Test
    fun `a debuggable non-system app is Flagged`() = runTest {
        val result = analyzer.analyze(appTarget(isDebuggable = true))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }

    @Test
    fun `flagged at INFO severity - common and often legitimate on its own`() = runTest {
        val result = analyzer.analyze(appTarget(isDebuggable = true))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().riskLevel).isEqualTo(RiskLevel.INFO)
    }

    @Test
    fun `a non-debuggable app is Clean`() = runTest {
        val result = analyzer.analyze(appTarget(isDebuggable = false))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a debuggable system app is still Clean`() = runTest {
        val result = analyzer.analyze(appTarget(isDebuggable = true, isSystemApp = true))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }
}
