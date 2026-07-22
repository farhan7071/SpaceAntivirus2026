plugins {
    id("spaceav.android.library")
    id("spaceav.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.space.antivirus.core.network"
}

dependencies {
    implementation(libs.bundles.network)
}
