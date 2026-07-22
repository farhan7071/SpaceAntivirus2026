plugins {
    id("spaceav.android.library")
    id("spaceav.android.hilt")
}

android {
    namespace = "com.space.antivirus.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(libs.datastore.preferences)
    // BUGFIX (Sprint 003.5 recovery): DataModule.kt calls
    // Room.databaseBuilder(...) directly, but this module only depended
    // on :core:database transitively, whose own Room dependency is
    // `implementation`-scoped (correctly encapsulated, not exposed
    // downstream). That left androidx.room.Room unresolved here — a real
    // compile error, not a hypothetical one. See Engineering Recovery Report.
    implementation(libs.bundles.room)
}
