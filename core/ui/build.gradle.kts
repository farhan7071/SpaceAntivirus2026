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
    // Sprint 034 (Part 8 — expand/collapse animation on ThreatSummaryCard).
    // Not covered by AndroidLibraryComposeConventionPlugin's default set
    // (compose-ui/material3/graphics only) — added here, specific to the
    // one module that actually needs it, rather than in the convention
    // plugin shared by every Compose module in this project.
    implementation(libs.compose.animation)

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
}
