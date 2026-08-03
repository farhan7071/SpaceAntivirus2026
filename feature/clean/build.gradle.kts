plugins {
    id("spaceav.android.feature")
}

android {
    namespace = "com.space.antivirus.feature.clean"

    // Same gap fixed for every prior feature module (Sprints 017-021) —
    // no shared default exists in the feature convention plugin.
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // Sprint 038 — expand/collapse on the junk-breakdown category rows.
    // Same precedent and same reasoning as core:ui's own addition
    // (Sprint 034, ThreatSummaryCard): not covered by
    // AndroidLibraryComposeConventionPlugin's default set (compose-ui/
    // material3/graphics only), so it's added to the one module that
    // actually needs it rather than to the convention plugin shared by
    // every Compose module in this project.
    implementation(libs.compose.animation)

    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
}
