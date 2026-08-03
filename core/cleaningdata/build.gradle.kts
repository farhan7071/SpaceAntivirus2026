plugins {
    id("spaceav.android.library")
    id("spaceav.android.hilt")
}

android {
    namespace = "com.space.antivirus.core.cleaningdata"

    // Same reasoning as core:database (Sprint 010), core:securitydata
    // (Sprint 011) and core:trusteddata (Sprint 012): real DAO-backed
    // tests need a genuine SQLite/Android environment. See ADR 0025.
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":domain"))

    // AppPrivateStorageRootsTest walks real temp directories with plain
    // JUnit — the containment guard is pure path logic and deliberately
    // testable without an emulator.
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)

    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.turbine)
}
