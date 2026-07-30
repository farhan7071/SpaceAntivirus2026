package com.space.antivirus.core.analysisengine.analyzer

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AppCategory
import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.InstalledApplicationInfo
import org.junit.Test

class ConfidenceModulationTest {

    private fun app(installerPackageName: String? = null, category: AppCategory = AppCategory.UNDEFINED) =
        InstalledApplicationInfo(
            packageName = "com.example.app",
            appLabel = "Example",
            versionName = "1.0",
            versionCode = 1L,
            installedAtEpochMillis = 0L,
            isSystemApp = false,
            apkPath = "/data/app/example.apk",
            requestedPermissions = emptyList(),
            installerPackageName = installerPackageName,
            category = category,
        )

    @Test
    fun `no legitimacy signal leaves confidence unchanged`() {
        val result = ConfidenceModulation.modulate(
            base = Confidence.MODERATE,
            app = app(installerPackageName = null, category = AppCategory.UNDEFINED),
            categoryIsConsistent = false,
        )

        assertThat(result).isEqualTo(Confidence.MODERATE)
    }

    @Test
    fun `Play Store installer lowers MODERATE to LOW`() {
        val result = ConfidenceModulation.modulate(
            base = Confidence.MODERATE,
            app = app(installerPackageName = "com.android.vending"),
            categoryIsConsistent = false,
        )

        assertThat(result).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `Samsung Galaxy Store installer also lowers MODERATE to LOW`() {
        val result = ConfidenceModulation.modulate(
            base = Confidence.MODERATE,
            app = app(installerPackageName = "com.sec.android.app.samsungapps"),
            categoryIsConsistent = false,
        )

        assertThat(result).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `Xiaomi's app store installer also lowers MODERATE to LOW`() {
        val result = ConfidenceModulation.modulate(
            base = Confidence.MODERATE,
            app = app(installerPackageName = "com.xiaomi.mipicks"),
            categoryIsConsistent = false,
        )

        assertThat(result).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `an unrecognized installer does not lower confidence on its own`() {
        val result = ConfidenceModulation.modulate(
            base = Confidence.MODERATE,
            app = app(installerPackageName = "com.some.unknown.store"),
            categoryIsConsistent = false,
        )

        assertThat(result).isEqualTo(Confidence.MODERATE)
    }

    @Test
    fun `a consistent category alone lowers confidence, independent of installer`() {
        val result = ConfidenceModulation.modulate(
            base = Confidence.MODERATE,
            app = app(installerPackageName = null),
            categoryIsConsistent = true,
        )

        assertThat(result).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `both a trusted installer and a consistent category only lower confidence once, not twice`() {
        // Both are evidence toward the SAME underlying question ("is this
        // expected behavior for this app?") - not independently additive.
        val result = ConfidenceModulation.modulate(
            base = Confidence.MODERATE,
            app = app(installerPackageName = "com.android.vending"),
            categoryIsConsistent = true,
        )

        assertThat(result).isEqualTo(Confidence.LOW)
    }

    @Test
    fun `HIGH steps down to MODERATE, not all the way to LOW`() {
        val result = ConfidenceModulation.modulate(
            base = Confidence.HIGH,
            app = app(installerPackageName = "com.android.vending"),
            categoryIsConsistent = false,
        )

        assertThat(result).isEqualTo(Confidence.MODERATE)
    }

    @Test
    fun `LOW never goes below LOW - there is no lower tier to step down to`() {
        val result = ConfidenceModulation.modulate(
            base = Confidence.LOW,
            app = app(installerPackageName = "com.android.vending"),
            categoryIsConsistent = false,
        )

        assertThat(result).isEqualTo(Confidence.LOW)
    }
}
