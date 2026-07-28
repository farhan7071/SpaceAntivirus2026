plugins {
    id("spaceav.android.library.compose")
}

android {
    namespace = "com.space.antivirus.core.ui"

    // Same gap fixed for every feature module since Sprint 017 — no
    // shared default exists in the library convention plugin. core:ui
    // never needed this before Sprint 030's ThreatSummaryCard, its first
    // component substantial enough to warrant a real Compose UI test.
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.compose.material.icons.extended)

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
}
