plugins {
    id("spaceav.android.library")
    id("spaceav.android.hilt")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.space.antivirus.core.database"

    // Only :app had this configured before Sprint 010 (via
    // AndroidApplicationConventionPlugin) — no library module needed an
    // instrumentation runner until this sprint's real DAO tests. Scoped
    // to this module specifically rather than added to the shared
    // spaceav.android.library convention plugin, since no other library
    // module has an androidTest source set yet.
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    testImplementation(libs.room.testing)

    // Real DAO tests (Sprint 010) need an actual SQLite/Android
    // environment — this project has no Robolectric set up, so these are
    // androidTest (instrumented), not JVM unit tests. They run during
    // physical-device verification, not `./gradlew test`. See ADR 0023.
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
}
