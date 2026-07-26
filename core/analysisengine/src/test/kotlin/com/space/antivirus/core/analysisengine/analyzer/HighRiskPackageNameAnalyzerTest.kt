package com.space.antivirus.core.analysisengine.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanTarget
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HighRiskPackageNameAnalyzerTest {

    private val analyzer = HighRiskPackageNameAnalyzer()

    private fun appTarget(packageName: String, isSystemApp: Boolean = false) = ScanTarget.ApplicationTarget(
        InstalledApplicationInfo(
            packageName = packageName,
            appLabel = "Example",
            versionName = "1.0",
            versionCode = 1L,
            installedAtEpochMillis = 0L,
            isSystemApp = isSystemApp,
            apkPath = "/data/app/example.apk",
            requestedPermissions = emptyList(),
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
    fun `the com-google-android namespace is also covered`() = runTest {
        val result = analyzer.analyze(appTarget(packageName = "com.google.android.fakegmail"))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }

    @Test
    fun `the evidence names both the matched namespace and the real package name`() = runTest {
        val result = analyzer.analyze(appTarget(packageName = "com.android.fakesystemupdate"))

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        val evidence = outcome.detections.single().evidenceDescription
        assertThat(evidence).contains("com.android.fakesystemupdate")
        assertThat(evidence).contains("com.android.")
    }
}
