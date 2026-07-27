package com.space.antivirus.core.analysisengine.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AppCategory
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanTarget
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SurveillanceCombinationAnalyzerTest {

    private val analyzer = SurveillanceCombinationAnalyzer()

    private fun appTarget(
        permissions: List<String> = emptyList(),
        isSystemApp: Boolean = false,
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
            category = category,
        ),
    )

    private val allThree = listOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.INTERNET",
    )

    @Test
    fun `camera plus record_audio plus internet is Flagged`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = allThree))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }

    @Test
    fun `only two of the three permissions is Clean`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.CAMERA", "android.permission.INTERNET")),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `no relevant permissions is Clean`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = emptyList()))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a system app with all three permissions is still Clean`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = allThree, isSystemApp = true))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a VIDEO category app with all three permissions is Clean - Sprint 028 category fix`() = runTest {
        // Directly the sprint's own worked example: a video-calling app
        // legitimately needing camera+microphone+internet shouldn't be
        // flagged at all, not flagged with softer wording.
        val result = analyzer.analyze(appTarget(permissions = allThree, category = AppCategory.VIDEO))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a SOCIAL category app with all three permissions is Clean - Sprint 028 category fix`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = allThree, category = AppCategory.SOCIAL))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a PRODUCTIVITY category app with all three permissions is still Flagged - suppression is category-specific`() =
        runTest {
            // Only VIDEO/SOCIAL are suppressed — this isn't a blanket
            // "any declared category excuses the finding" rule.
            val result = analyzer.analyze(appTarget(permissions = allThree, category = AppCategory.PRODUCTIVITY))

            assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
        }

    @Test
    fun `UNDEFINED category (the default - no declared category) is still Flagged`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = allThree, category = AppCategory.UNDEFINED))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }
}
