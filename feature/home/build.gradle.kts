plugins {
    id("spaceav.android.feature")
}

android {
    namespace = "com.space.antivirus.feature.home"

    // Same gap found and fixed in every prior library module needing
    // androidTest (core:database Sprint 010, core:securitydata/trusteddata
    // Sprint 011/012) — no shared default exists in the library convention
    // plugins, only :app had one before this project's UI work began.
    // Plain AndroidJUnitRunner here, not a Hilt one: this sprint's UI test
    // exercises the stateless HomeScreen composable directly with a
    // hand-built HomeUiState, never touching hiltViewModel()/DI at all.
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
