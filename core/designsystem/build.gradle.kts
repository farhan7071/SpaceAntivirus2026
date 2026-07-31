plugins {
    id("spaceav.android.library.compose")
}

android {
    namespace = "com.space.antivirus.core.designsystem"

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Sprint 035 (SDS v1.0, Part 7 — Icon tokens). Not covered by
    // AndroidLibraryComposeConventionPlugin's default set (compose-ui/
    // material3/graphics only) — core:ui already depends on this for
    // the same reason (ADR 0031); this module needs it too now that the
    // icon token layer lives here, alongside every other token file
    // rather than split across modules.
    implementation(libs.compose.material.icons.extended)
}
