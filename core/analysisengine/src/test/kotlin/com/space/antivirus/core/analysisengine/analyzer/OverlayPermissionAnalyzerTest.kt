package com.space.antivirus.core.analysisengine.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanTarget
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OverlayPermissionAnalyzerTest {

    private val analyzer = OverlayPermissionAnalyzer()

    private fun appTarget(
        permissions: List<String> = emptyList(),
        isSystemApp: Boolean = false,
    ) = ScanTarget.ApplicationTarget(
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
    fun `overlay plus internet is Flagged`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.SYSTEM_ALERT_WINDOW", "android.permission.INTERNET")),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }

    @Test
    fun `overlay alone without internet is Clean`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = listOf("android.permission.SYSTEM_ALERT_WINDOW")))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `internet alone without overlay is Clean`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = listOf("android.permission.INTERNET")))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a system app with both permissions is still Clean`() = runTest {
        val result = analyzer.analyze(
            appTarget(
                permissions = listOf("android.permission.SYSTEM_ALERT_WINDOW", "android.permission.INTERNET"),
                isSystemApp = true,
            ),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `the detection is attributed to this analyzer's own id`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.SYSTEM_ALERT_WINDOW", "android.permission.INTERNET")),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().analyzerId).isEqualTo(analyzer.id)
    }
}
