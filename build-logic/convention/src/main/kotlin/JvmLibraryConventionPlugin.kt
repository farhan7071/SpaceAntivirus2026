import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Applied by :domain — deliberately has zero Android framework dependency
 *  so use cases stay unit-testable as plain Kotlin. See
 *  docs/adr/0005-domain-module-is-pure-kotlin.md */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            dependencies {
                // NOTE (Sprint 003.5 recovery): build-logic is a separate
                // Gradle build and can't cleanly read the root project's
                // version catalog for its own dependencies (see the
                // literal AGP/Kotlin versions in build-logic's own
                // build.gradle.kts for the same reason) — pin and comment
                // this so it's an intentional, visible choice rather than
                // silent drift from gradle/libs.versions.toml's junit = "4.13.2".
                add("testImplementation", "junit:junit:4.13.2")
            }
        }
    }
}
