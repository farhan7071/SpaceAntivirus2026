plugins {
    id("spaceav.android.library")
    id("spaceav.android.hilt")
}

android {
    namespace = "com.space.antivirus.core.protection"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":domain"))

    // NotificationCompat / NotificationManagerCompat / ContextCompat.
    implementation(libs.androidx.core.ktx)

    // ProtectionManagerImpl is deliberately free of Android framework
    // types so its ordering guarantees — persist only after a confirmed
    // schedule, never notify before that — can be tested on the JVM.
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
}
