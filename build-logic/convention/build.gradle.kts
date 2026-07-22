plugins {
    `kotlin-dsl`
}

group = "com.space.antivirus.buildlogic"

// Pulled from the root version catalog so build-logic and the app modules
// never drift on AGP/Kotlin versions independently.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("com.android.tools.build:gradle:8.7.2")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
    compileOnly("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.0.21")
}

gradlePlugin {
    plugins {
        register("androidFeature") {
            id = "spaceav.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidLibrary") {
            id = "spaceav.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "spaceav.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "spaceav.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "spaceav.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "spaceav.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = "spaceav.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
