plugins {
    id("spaceav.jvm.library")
}

// BUGFIX (Sprint 003.5 recovery): this module used to apply
// spaceav.android.library, but nothing in it touches an Android API
// (AppResult.kt, DispatcherProvider.kt are both plain Kotlin). That made
// :domain (a spaceav.jvm.library module) depend on an Android Library
// module's AAR output, which Gradle cannot resolve — a fatal
// configuration-time failure. See docs/adr/0011-core-common-and-core-model-are-pure-kotlin.md
dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
