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
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
}
