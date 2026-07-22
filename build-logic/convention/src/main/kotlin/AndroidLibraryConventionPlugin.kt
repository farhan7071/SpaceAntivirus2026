import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Applied by every `core:*` module. Centralizes compileSdk/minSdk/Kotlin
 * options so raising the target SDK (Sprint 001 Risk #1 / Sprint 002 §9)
 * is a one-line change here, not a 20-module find-and-replace.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                compileSdk = 36 // Android 16 — see docs/adr/0002-target-sdk-36.md

                defaultConfig {
                    minSdk = 26 // see docs/adr/0003-minimum-sdk-26.md
                }

                compileOptions {
                    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                    targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
                }
            }

            tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                    freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
                }
            }

            dependencies {
                // BUGFIX (Sprint 003.5 recovery, see Engineering Recovery
                // Report + docs/adr/0012): this used to run unconditionally,
                // which meant :core:testing itself received a
                // testImplementation dependency on :core:testing — a
                // project depending on itself. Every other core:* module
                // still gets it as before.
                if (target.path != ":core:testing") {
                    add("testImplementation", project(":core:testing"))
                }
            }
        }
    }
}
