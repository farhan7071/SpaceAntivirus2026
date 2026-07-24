package com.space.antivirus.core.analysisengine.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.core.model.ThreatType
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppIdentityImpersonationAnalyzerTest {

    private val analyzer = AppIdentityImpersonationAnalyzer()

    private fun appTarget(
        appLabel: String,
        packageName: String,
        isSystemApp: Boolean = false,
    ) = ScanTarget.ApplicationTarget(
        InstalledApplicationInfo(
            packageName = packageName,
            appLabel = appLabel,
            versionName = "1.0",
            versionCode = 1L,
            installedAtEpochMillis = 0L,
            isSystemApp = isSystemApp,
            apkPath = "/data/app/example.apk",
            requestedPermissions = emptyList(),
        ),
    )

    // --- identity ---

    @Test
    fun `id and capabilities are stable and correct`() {
        assertThat(analyzer.id).isEqualTo(AnalyzerId("app-identity-impersonation"))
        assertThat(analyzer.capabilities).containsExactly(AnalyzerCapability.APPLICATION_ANALYSIS)
    }

    // --- safe scenarios ---

    @Test
    fun `the genuine WhatsApp package is Clean`() = runTest {
        val result = analyzer.analyze(appTarget(appLabel = "WhatsApp", packageName = "com.whatsapp"))

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `an app with a label not on the known-brand list is Clean, regardless of package name`() = runTest {
        val result = analyzer.analyze(
            appTarget(appLabel = "My Cool Utility App", packageName = "com.random.developer.tool"),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a partial or substring label match does not trigger the check`() = runTest {
        // "WhatsApp Backup Tool" is not an exact match to "WhatsApp" —
        // must not be flagged just for mentioning the brand name.
        val result = analyzer.analyze(
            appTarget(appLabel = "WhatsApp Backup Tool", packageName = "com.thirdparty.backup"),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a system app impersonating a known brand is still Clean`() = runTest {
        val result = analyzer.analyze(
            appTarget(appLabel = "WhatsApp", packageName = "com.not.the.real.one", isSystemApp = true),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    // --- impersonation detected ---

    @Test
    fun `a label matching WhatsApp with a different package is Flagged`() = runTest {
        val result = analyzer.analyze(
            appTarget(appLabel = "WhatsApp", packageName = "com.definitely.not.whatsapp"),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections).hasSize(1)
        assertThat(outcome.detections.first().threatType).isEqualTo(ThreatType.POTENTIALLY_UNWANTED_APPLICATION)
    }

    @Test
    fun `every known brand in the list is checked correctly`() = runTest {
        val knownBrands = mapOf(
            "WhatsApp" to "com.whatsapp",
            "Instagram" to "com.instagram.android",
            "Facebook" to "com.facebook.katana",
            "Google Chrome" to "com.android.chrome",
            "Google Play Store" to "com.android.vending",
        )

        for ((label, realPackage) in knownBrands) {
            val genuineResult = analyzer.analyze(appTarget(appLabel = label, packageName = realPackage))
            assertThat((genuineResult as AppResult.Success).data)
                .isInstanceOf(AnalysisOutcome.Clean::class.java)

            val impersonatingResult = analyzer.analyze(appTarget(appLabel = label, packageName = "com.fake.app"))
            assertThat((impersonatingResult as AppResult.Success).data)
                .isInstanceOf(AnalysisOutcome.Flagged::class.java)
        }
    }

    @Test
    fun `the evidence description names both the claimed and real package identity`() = runTest {
        val result = analyzer.analyze(
            appTarget(appLabel = "Instagram", packageName = "com.fake.instagram.clone"),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        val evidence = outcome.detections.first().evidenceDescription
        assertThat(evidence).contains("com.fake.instagram.clone")
        assertThat(evidence).contains("com.instagram.android")
    }

    @Test
    fun `every detection is attributed to this analyzer's own id`() = runTest {
        val result = analyzer.analyze(
            appTarget(appLabel = "WhatsApp", packageName = "com.fake.app"),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.first().analyzerId).isEqualTo(analyzer.id)
    }

    // --- wrong target type ---

    @Test
    fun `a FileTarget is rejected with InvalidScanConfiguration, not a crash`() = runTest {
        val fileTarget = ScanTarget.FileTarget(
            FileMetadata(
                path = "/downloads/file.txt",
                name = "file.txt",
                sizeBytes = 10L,
                mimeType = "text/plain",
                lastModifiedEpochMillis = 0L,
                isDirectory = false,
            ),
        )

        val result = analyzer.analyze(fileTarget)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        assertThat((result as AppResult.Failure).error).isInstanceOf(AppError.InvalidScanConfiguration::class.java)
    }
}
