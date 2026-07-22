plugins {
    id("spaceav.android.feature")
}

android {
    namespace = "com.space.antivirus.feature.home"
}

dependencies {
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}
