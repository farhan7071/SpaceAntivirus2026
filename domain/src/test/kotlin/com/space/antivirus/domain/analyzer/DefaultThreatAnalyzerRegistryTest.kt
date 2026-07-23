package com.space.antivirus.domain.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.fake.FakeThreatAnalyzer
import org.junit.Test

class DefaultThreatAnalyzerRegistryTest {

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

    private val appTarget = ScanTarget.ApplicationTarget(
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

    private fun analyzer(id: String, vararg capabilities: AnalyzerCapability) = FakeThreatAnalyzer(
        id = AnalyzerId(id),
        capabilities = capabilities.toSet(),
        result = AppResult.Success(AnalysisOutcome.Clean("unused")),
    )

    @Test
    fun `allAnalyzers returns every registered analyzer regardless of capability`() {
        val fileAnalyzer = analyzer("file-analyzer", AnalyzerCapability.FILE_ANALYSIS)
        val appAnalyzer = analyzer("app-analyzer", AnalyzerCapability.APPLICATION_ANALYSIS)
        val registry = DefaultThreatAnalyzerRegistry(setOf(fileAnalyzer, appAnalyzer))

        assertThat(registry.allAnalyzers()).containsExactly(fileAnalyzer, appAnalyzer)
    }

    @Test
    fun `analyzersFor routes a FileTarget only to FILE_ANALYSIS-capable analyzers`() {
        val fileAnalyzer = analyzer("file-analyzer", AnalyzerCapability.FILE_ANALYSIS)
        val appAnalyzer = analyzer("app-analyzer", AnalyzerCapability.APPLICATION_ANALYSIS)
        val registry = DefaultThreatAnalyzerRegistry(setOf(fileAnalyzer, appAnalyzer))

        assertThat(registry.analyzersFor(fileTarget)).containsExactly(fileAnalyzer)
        assertThat(registry.analyzersFor(appTarget)).containsExactly(appAnalyzer)
    }

    @Test
    fun `an empty registry returns no analyzers for any target`() {
        val registry = DefaultThreatAnalyzerRegistry(emptySet())

        assertThat(registry.analyzersFor(fileTarget)).isEmpty()
        assertThat(registry.allAnalyzers()).isEmpty()
    }

    @Test
    fun `an analyzer with both capabilities is returned for both target types`() {
        val universalAnalyzer = analyzer(
            "universal",
            AnalyzerCapability.FILE_ANALYSIS,
            AnalyzerCapability.APPLICATION_ANALYSIS,
        )
        val registry = DefaultThreatAnalyzerRegistry(setOf(universalAnalyzer))

        assertThat(registry.analyzersFor(fileTarget)).containsExactly(universalAnalyzer)
        assertThat(registry.analyzersFor(appTarget)).containsExactly(universalAnalyzer)
    }
}
