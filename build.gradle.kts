// Root build.gradle.kts — declares plugins at the root without applying them
// (apply false), so every subproject applies only the plugins it needs.
// This is the standard AGP8+/Kotlin2+ convention and avoids the old
// buildscript{} classpath block entirely.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Applies static-analysis tooling identically to every module from one
// place — see docs/adr/0006-static-analysis-tooling.md for why this is
// configured at the root rather than per-module.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        autoCorrect = false
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
