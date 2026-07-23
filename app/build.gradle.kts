plugins {
    id("spaceav.android.application")
    id("spaceav.android.application.compose")
    id("spaceav.android.hilt")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:security"))
    implementation(project(":core:permissions"))
    implementation(project(":core:enumeration"))
    implementation(project(":domain"))

    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:security"))
    implementation(project(":feature:clean"))
    implementation(project(":feature:history"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:premium"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:realtime"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.profileinstaller)
    // BUGFIX (Sprint 003.5 recovery): TopLevelDestination.kt uses
    // Icons.Filled.Security and Icons.Filled.CleaningServices, which are
    // NOT part of the default material-icons-core set bundled with
    // compose-material3 — they require material-icons-extended. This
    // module previously relied on core:ui's *implementation*-scoped
    // dependency on that artifact, which is not visible here. See
    // Engineering Recovery Report.
    implementation(libs.compose.material.icons.extended)
    // The Android-specific Dispatchers.Main implementation is needed
    // somewhere in the final app's runtime classpath; core:common (now a
    // pure-Kotlin module, see ADR 0011) intentionally only depends on
    // kotlinx-coroutines-core, so :app supplies the Android integration.
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
