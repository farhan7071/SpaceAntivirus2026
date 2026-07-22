import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/** Applies Hilt + KSP identically wherever DI is needed — see
 *  docs/adr/0001-hilt-for-dependency-injection.md */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("com.google.dagger.hilt.android")
            }

            val libs = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
                .named("libs")

            dependencies {
                add("implementation", libs.findLibrary("hilt-android").get())
                add("ksp", libs.findLibrary("hilt-compiler").get())
            }
        }
    }
}
