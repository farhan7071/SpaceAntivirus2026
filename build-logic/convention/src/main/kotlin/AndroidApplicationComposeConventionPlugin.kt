import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Adds Compose config on top of spaceav.android.application. Exists
 * because spaceav.android.library.compose applies spaceav.android.library
 * internally (com.android.library) — applying that to :app, which already
 * applies spaceav.android.application (com.android.application), is
 * illegal: AGP does not allow both plugins in the same project. This is
 * the application-module counterpart, applying spaceav.android.application
 * instead. See docs/adr/0013-application-compose-convention-plugin.md.
 */
class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("spaceav.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ApplicationExtension> {
                buildFeatures {
                    compose = true
                }
            }

            val libs = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
                .named("libs")

            dependencies {
                val bom = libs.findLibrary("compose-bom").get()
                add("implementation", platform(bom))
                add("androidTestImplementation", platform(bom))
                add("implementation", libs.findLibrary("compose-ui").get())
                add("implementation", libs.findLibrary("compose-ui-graphics").get())
                add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
                add("implementation", libs.findLibrary("compose-material3").get())
                add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
                add("debugImplementation", libs.findLibrary("compose-ui-test-manifest").get())
            }
        }
    }
}
