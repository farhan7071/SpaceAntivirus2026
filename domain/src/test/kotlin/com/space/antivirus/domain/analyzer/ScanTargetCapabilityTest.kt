package com.space.antivirus.domain.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanTarget
import org.junit.Test

class ScanTargetCapabilityTest {

    private val fileTarget = ScanTarget.FileTarget(
        FileMetadata(
            path = "/downloads/file.apk",
            name = "file.apk",
            sizeBytes = 100L,
            mimeType = "application/vnd.android.package-archive",
            lastModifiedEpochMillis = 0L,
            isDirectory = false,
        ),
    )

    private val applicationTarget = ScanTarget.ApplicationTarget(
        InstalledApplicationInfo(
            packageName = "com.example.app",
            appLabel = "Example",
            versionName = "1.0",
            versionCode = 1L,
            installedAtEpochMillis = 0L,
            isSystemApp = false,
            apkPath = "/data/app/example.apk",
        ),
    )

    @Test
    fun `FileTarget requires FILE_ANALYSIS capability`() {
        assertThat(fileTarget.requiredCapability).isEqualTo(AnalyzerCapability.FILE_ANALYSIS)
    }

    @Test
    fun `ApplicationTarget requires APPLICATION_ANALYSIS capability`() {
        assertThat(applicationTarget.requiredCapability).isEqualTo(AnalyzerCapability.APPLICATION_ANALYSIS)
    }
}
