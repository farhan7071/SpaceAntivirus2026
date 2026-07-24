plugins {
    id("spaceav.android.library")
    id("spaceav.android.hilt")
}

android {
    namespace = "com.space.antivirus.core.securitydata"

    // Real SecurityRepositoryImpl tests (Sprint 011) need a genuine
    // SQLite/Room environment, same reasoning as core:database's Sprint
    // 010 DAO tests — this project has no Robolectric configured. See
    // ADR 0024.
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":domain"))
    // BUGFIX-avoidance, learned directly from Sprint 003.5's recovery
    // (core:data needed Room declared directly, not just transitively
    // through core:database's implementation-scoped dependency): this
    // module calls androidx.room.withTransaction directly, so it needs
    // room-ktx on its OWN compile classpath, not just core:database's.
    implementation(libs.bundles.room)

    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.turbine)
}
