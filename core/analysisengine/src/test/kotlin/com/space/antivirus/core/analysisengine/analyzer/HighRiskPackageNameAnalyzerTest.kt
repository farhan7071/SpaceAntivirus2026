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

class HighRiskPackageNameAnalyzerTest {

    private val analyzer = HighRiskPackageNameAnalyzer()

    private fun appTarget(
        packageName: String,
        isSystemApp: Boolean = false,
        installerPackageName: String? = null,
    ) = ScanTarget.ApplicationTarget(
        InstalledApplicationInfo(
            packageName = packageName,
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
    fun `a non-system app claiming the com-android namespace is Flagged`() = runTest {
        val result = analyzer.analyze(appTarget(packageName = "com.android.fakesystemupdate"))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }

    @Test
    fun `a genuine system app claiming the com-android namespace is Clean`() = runTest {
        val result = analyzer.analyze(appTarget(packageName = "com.android.settings", isSystemApp = true))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `an ordinary third-party package name is Clean`() = runTest {
        val result = analyzer.analyze(appTarget(packageName = "com.example.myapp"))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a package merely containing the word android is not flagged - exact prefix only`() = runTest {
        val result = analyzer.analyze(appTarget(packageName = "com.mycompany.androidutils"))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `com-google-android is no longer a reserved namespace - Sprint 028 false-positive fix`() = runTest {
        // Real-device testing found this exact pattern false-flagging
        // genuine, Play-Store-distributed Google apps (Gmail as
        // com.google.android.gm, YouTube as com.google.android.youtube) —
        // isSystemApp=false is completely normal for a Google app once
        // it's been updated via the Play Store, so the system-app
        // exclusion this analyzer already had couldn't protect against
        // exactly the apps most likely to trip this rule.
        val result = analyzer.analyze(appTarget(packageName = "com.google.android.gm"))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `com-android- and android- remain reserved - the fix removed one namespace, not the whole rule`() = runTest {
        val comAndroidResult = analyzer.analyze(appTarget(packageName = "com.android.fakeupdate"))
        val androidResult = analyzer.analyze(appTarget(packageName = "android.fakecomponent"))

        assertThat((comAndroidResult as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
        assertThat((androidResult as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }

    @Test
    fun `the evidence names both the matched namespace and the real package name`() = runTest {
        val result = analyzer.analyze(appTarget(packageName = "com.android.fakesystemupdate"))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        val evidence = outcome.detections.single().evidenceDescription
        assertThat(evidence).contains("com.android.fakesystemupdate")
        assertThat(evidence).contains("com.android.")
    }

    // --- Sprint 032 regression: namespace-impersonation detection is unaffected by ConfidenceModulation ---

    @Test
    fun `confidence stays HIGH even from a trusted app store - not downgraded like the permission-behavior analyzers`() =
        runTest {
            // Same reasoning as AppIdentityImpersonationAnalyzer's identical
            // regression test: a non-system app squatting a reserved
            // system namespace has no legitimate excuse regardless of
            // which store it came from.
            val result = analyzer.analyze(
                appTarget(
                    packageName = "com.android.fakesystemupdate",
                    installerPackageName = "com.android.vending",
                ),
            )

            val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
            assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.HIGH)
            assertThat(outcome.detections.single().riskLevel).isEqualTo(RiskLevel.ATTENTION)
        }
}
