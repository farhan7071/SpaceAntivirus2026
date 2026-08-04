plugins {
    id("spaceav.android.library")
    id("spaceav.android.hilt")
}

android {
    namespace = "com.space.antivirus.core.data"
}

dependencies {
    // Sprint 043A: PackageInfoCompat.getLongVersionCode, which handles the
    // API 28 versionCode/longVersionCode split without a manual SDK check.
    implementation(libs.androidx.core.ktx)

    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    // Sprint 026: core:data implements a domain contract
    // (BackgroundProtectionPreferences) for the first time — every
    // prior module implementing a domain interface already depended on
    // :domain (core:enumeration, core:securitydata, etc. since Sprint
    // 004B); this module simply hadn't needed to before now.
    implementation(project(":domain"))
    implementation(libs.datastore.preferences)
    // BUGFIX (Sprint 003.5 recovery): DataModule.kt calls
    // Room.databaseBuilder(...) directly, but this module only depended
    // on :core:database transitively, whose own Room dependency is
    // `implementation`-scoped (correctly encapsulated, not exposed
    // downstream). That left androidx.room.Room unresolved here — a real
    // compile error, not a hypothetical one. See Engineering Recovery Report.
    implementation(libs.bundles.room)
}
