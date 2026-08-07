plugins {
    id("spaceav.android.library.compose")
    id("spaceav.android.hilt")
}

android {
    namespace = "com.space.antivirus.core.ads"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":domain"))

    // The ONLY module in this project that may reference the Google
    // Mobile Ads SDK. Everything outside core:ads talks to the
    // interfaces in this module's api surface instead, so the SDK can be
    // swapped, stubbed or removed without touching a feature module.
    implementation(libs.play.services.ads)

    // Sprint 049. The certified consent platform. Stays inside core:ads
    // for the same reason the ads SDK does: nothing outside this module
    // sees a Google type.
    implementation(libs.user.messaging.platform)

    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
