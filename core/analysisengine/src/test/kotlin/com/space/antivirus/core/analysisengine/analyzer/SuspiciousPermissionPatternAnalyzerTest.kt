package com.space.antivirus.core.analysisengine.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.AppCategory
import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanTarget
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SuspiciousPermissionPatternAnalyzerTest {

    private val analyzer = SuspiciousPermissionPatternAnalyzer()

    private fun appTarget(
        packageName: String = "com.example.app",
        isSystemApp: Boolean = false,
        permissions: List<String> = emptyList(),
        installerPackageName: String? = null,
        category: AppCategory = AppCategory.UNDEFINED,
    ) = ScanTarget.ApplicationTarget(
        InstalledApplicationInfo(
            packageName = packageName,
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

    // --- identity ---

    @Test
    fun `id and capabilities are stable and correct`() {
        assertThat(analyzer.id).isEqualTo(AnalyzerId("suspicious-permission-pattern"))
        assertThat(analyzer.capabilities).containsExactly(AnalyzerCapability.APPLICATION_ANALYSIS)
    }

    // --- safe scenarios ---

    @Test
    fun `an app with no permissions is Clean`() = runTest {
        val result = analyzer.analyze(appTarget(permissions = emptyList()))

        assertThat(result).isInstanceOf(AppResult.Success::class.java)
        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a typical camera app permission set is Clean`() = runTest {
        val result = analyzer.analyze(
            appTarget(
                permissions = listOf(
                    "android.permission.CAMERA",
                    "android.permission.INTERNET",
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                ),
            ),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `SMS permission alone, without INTERNET, is Clean`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.READ_SMS")),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `INTERNET permission alone, without SMS or device admin, is Clean`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.INTERNET")),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    @Test
    fun `a system app with the SMS plus INTERNET pattern is still Clean`() = runTest {
        val result = analyzer.analyze(
            appTarget(
                isSystemApp = true,
                permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
            ),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    // --- SMS interception pattern ---

    @Test
    fun `READ_SMS plus INTERNET is Flagged with one detection`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET")),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections).hasSize(1)
        assertThat(outcome.detections.first().evidenceDescription).contains("SMS")
    }

    @Test
    fun `RECEIVE_SMS plus INTERNET also matches the SMS interception pattern`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.RECEIVE_SMS", "android.permission.INTERNET")),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Flagged::class.java)
    }

    // --- device admin lock pattern ---

    @Test
    fun `BIND_DEVICE_ADMIN plus INTERNET is Flagged with one detection`() = runTest {
        val result = analyzer.analyze(
            appTarget(
                permissions = listOf("android.permission.BIND_DEVICE_ADMIN", "android.permission.INTERNET"),
            ),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections).hasSize(1)
        assertThat(outcome.detections.first().evidenceDescription).contains("device administrator")
    }

    @Test
    fun `BIND_DEVICE_ADMIN alone, without INTERNET, is Clean`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.BIND_DEVICE_ADMIN")),
        )

        assertThat((result as AppResult.Success).data).isInstanceOf(AnalysisOutcome.Clean::class.java)
    }

    // --- both patterns at once ---

    @Test
    fun `both patterns present produces two detections in one Flagged outcome`() = runTest {
        val result = analyzer.analyze(
            appTarget(
                permissions = listOf(
                    "android.permission.READ_SMS",
                    "android.permission.BIND_DEVICE_ADMIN",
                    "android.permission.INTERNET",
                ),
            ),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections).hasSize(2)
    }

    // --- every detection is correctly attributed and evidence-based ---

    @Test
    fun `every detection is attributed to this analyzer's own id`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET")),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.first().analyzerId).isEqualTo(analyzer.id)
    }

    @Test
    fun `the flagged outcome's targetIdentifier is the app's package name`() = runTest {
        val result = analyzer.analyze(
            appTarget(
                packageName = "com.suspicious.app",
                permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
            ),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.targetIdentifier).isEqualTo("com.suspicious.app")
    }

    // --- Sprint 031: confidence modulation (ADR 0045) ---

    @Test
    fun `SMS pattern default confidence is MODERATE with no legitimacy signal`() = runTest {
        val result = analyzer.analyze(
            appTarget(permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET")),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.MODERATE)
    }

    @Test
    fun `SMS pattern from the Play Store is downgraded to LOW confidence, still Flagged`() = runTest {
        val result = analyzer.analyze(
            appTarget(
                permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
                installerPackageName = "com.android.vending",
            ),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.LOW)
        // Evidence is preserved, not suppressed - goal #3's "preserve
        // evidence, continue informing the user."
        assertThat(outcome.detections.single().evidenceDescription).contains("SMS")
    }

    @Test
    fun `SMS pattern on a SOCIAL-category app is also downgraded to LOW confidence`() = runTest {
        val result = analyzer.analyze(
            appTarget(
                permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
                category = AppCategory.SOCIAL,
            ),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `SMS pattern on a PRODUCTIVITY-category app is NOT downgraded - only SOCIAL is consistent for this rule`() =
        runTest {
            val result = analyzer.analyze(
                appTarget(
                    permissions = listOf("android.permission.READ_SMS", "android.permission.INTERNET"),
                    category = AppCategory.PRODUCTIVITY,
                ),
            )

            val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
            assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.MODERATE)
        }

    @Test
    fun `device admin pattern from the Play Store is downgraded to LOW - installer trust applies to both rules`() =
        runTest {
            val result = analyzer.analyze(
                appTarget(
                    permissions = listOf("android.permission.BIND_DEVICE_ADMIN", "android.permission.INTERNET"),
                    installerPackageName = "com.android.vending",
                ),
            )

            val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
            assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.LOW)
        }

    @Test
    fun `device admin pattern is NOT downgraded by category - no category is consistent for this rule`() = runTest {
        val result = analyzer.analyze(
            appTarget(
                permissions = listOf("android.permission.BIND_DEVICE_ADMIN", "android.permission.INTERNET"),
                category = AppCategory.PRODUCTIVITY,
            ),
        )

        val outcome = (result as AppResult.Success).data as AnalysisOutcome.Flagged
        assertThat(outcome.detections.single().confidence).isEqualTo(Confidence.MODERATE)
    }

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
