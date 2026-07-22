plugins {
    id("spaceav.android.library")
}

android {
    namespace = "com.space.antivirus.core.testing"
}

dependencies {
    implementation(project(":core:common"))
    api(libs.bundles.test.unit)
    api(libs.androidx.test.ext.junit)
    api(libs.espresso.core)
}
