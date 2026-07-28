package com.space.antivirus.core.analysisengine.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AppCategory
import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanTarget
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OverlayPermissionAnalyzerTest {

    private val analyzer = OverlayPermissionAnalyzer()

    private fun appTarget(
        permissions: List<String> = emptyList(),
        isSystemApp: Boolean = false,
        installerPackageName: String? = null,
        category: AppCategory = AppCategory.UNDEFINED,
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
            installerPackageName = installerPackageName,
            category = category,
        ),
    )

    private val overlayPlusInternet = listOf("android.permission.SYSTEM_ALERT_WINDOW", "android.permission.INTERNET")

    @Test
    fun `overlay plus internet is Flagged`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = overlayPlusInternet))

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
        val result = analyzer.analyze(appTarget(permissions = overlayPlusInternet, isSystemApp = true))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `the detection is attributed to this analyzer's own id`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = overlayPlusInternet))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().analyzerId).isEqualTo(analyzer.id)
    }

    // --- Sprint 031: confidence modulation (ADR 0045) ---

    @Test
    fun `default confidence is MODERATE with no legitimacy signal`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = overlayPlusInternet))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.MODERATE)
    }

    @Test
    fun `installed from the Play Store downgrades confidence to LOW, still Flagged`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = overlayPlusInternet, installerPackageName = "com.android.vending"),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `SOCIAL category downgrades confidence - chat-head-style overlay use is expected`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = overlayPlusInternet, category = AppCategory.SOCIAL))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `GAME category also downgrades confidence - in-game overlays are expected`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = overlayPlusInternet, category = AppCategory.GAME))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `NEWS category does NOT downgrade confidence - not a category this analyzer treats as consistent`() =
        runTest {
            val result = analyzer.analyze(appTarget(permissions = overlayPlusInternet, category = AppCategory.NEWS))

            val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
            assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.MODERATE)
        }

    // --- Sprint 032: two more real, already-populated categories added ---

    @Test
    fun `AUDIO category downgrades confidence - floating media controls are expected on music apps`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = overlayPlusInternet, category = AppCategory.AUDIO))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `MAPS category downgrades confidence - navigation directions overlay is expected`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = overlayPlusInternet, category = AppCategory.MAPS))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.LOW)
    }
}
