import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Applied by every feature:* module. Bundles library+compose+hilt conventions
 * and wires the standard set of core:* dependencies every feature needs, so
 * a feature module's own build.gradle.kts stays to ~10 lines.
 * See docs/adr/0004-feature-module-boundaries.md for why features may not
 * depend on each other.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // BUGFIX (Sprint 003.5 recovery): previously applied
            // spaceav.android.library explicitly AND THEN
            // spaceav.android.library.compose, which applies
            // spaceav.android.library internally too — running the same
            // configure{} block twice. Not fatal (idempotent), but real
            // duplication caught during the recovery audit; removed.
            pluginManager.apply("spaceav.android.library.compose")
            pluginManager.apply("spaceav.android.hilt")

            val libs = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
                .named("libs")

            dependencies {
                add("implementation", project(":core:common"))
                add("implementation", project(":core:model"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":domain"))
                add("implementation", libs.findLibrary("navigation-compose").get())
                add("implementation", libs.findLibrary("hilt-navigation-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("testImplementation", project(":core:testing"))
            }
        }
    }
}
