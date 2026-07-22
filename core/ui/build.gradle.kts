plugins {
    id("spaceav.android.library.compose")
}

android {
    namespace = "com.space.antivirus.core.ui"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.compose.material.icons.extended)
}
