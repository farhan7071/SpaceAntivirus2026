import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Applied only by :app. Owns applicationId, versioning, and build variants. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                namespace = "com.space.antivirus.viruscleaner.mobilesecurity"
                compileSdk = 36

                defaultConfig {
                    applicationId = "com.space.antivirus.viruscleaner.mobilesecurity"
                    minSdk = 26
                    targetSdk = 36
                    versionCode = 16
                    versionName = "2.0.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                // Build variants: debug / internal / qa / release, per Sprint 003 Task 2.
                buildTypes {
                    debug {
                        applicationIdSuffix = ".debug"
                        isDebuggable = true
                    }
                    create("internal") {
                        initWith(getByName("debug"))
                        applicationIdSuffix = ".internal"
                        matchingFallbacks += listOf("debug")
                    }
                    create("qa") {
                        initWith(getByName("debug"))
                        applicationIdSuffix = ".qa"
                        isMinifyEnabled = true
                        matchingFallbacks += listOf("debug")
                    }
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                    }
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
        }
    }
}
