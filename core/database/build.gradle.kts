plugins {
    id("spaceav.android.library")
    id("spaceav.android.hilt")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.space.antivirus.core.database"
}

dependencies {
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    testImplementation(libs.room.testing)
}
