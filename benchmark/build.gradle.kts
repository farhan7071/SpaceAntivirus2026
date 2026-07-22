plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.space.antivirus.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 28 // Macrobenchmark's floor, higher than the app's own minSdk 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.espresso.core)
}
