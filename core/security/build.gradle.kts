plugins {
    id("spaceav.android.library")
    id("spaceav.android.hilt")
}

android {
    namespace = "com.space.antivirus.core.security"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.security.crypto)
}
