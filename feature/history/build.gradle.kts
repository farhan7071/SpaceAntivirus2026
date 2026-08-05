plugins {
    id("spaceav.android.feature")
}

android {
    namespace = "com.space.antivirus.feature.history"

    // Same gap fixed for every prior feature module (Sprints 017-020) —
    // no shared default exists in the feature convention plugin.
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // Sprint 044. The ONLY feature module that depends on core:ads,
    // because History carries this app's only banner placement. Feature
    // modules see core:ads' own interfaces; the Google SDK stays inside
    // that module.
    implementation(project(":core:ads"))

    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
}
