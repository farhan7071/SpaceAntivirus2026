pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SpaceAntivirus2026"

// -------------------------------------------------------------------------
// Module map — see docs/architecture.md for the "why" behind each module.
// -------------------------------------------------------------------------
include(":app")

// Core modules — no feature knows about another feature; everything shared
// lives here so feature modules never depend on each other directly.
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:ui")
include(":core:data")
include(":core:database")
include(":core:network")
include(":core:security")
include(":core:permissions")
include(":core:testing")
include(":core:enumeration")

// Domain — use cases that coordinate one or more core:data repositories.
// Deliberately has zero Android dependency (pure Kotlin/JVM module).
include(":domain")

// Feature modules — one per Sprint 002.5 top-level or nav-reachable screen.
// Each depends only on core:* and :domain, never on another feature.
include(":feature:onboarding")
include(":feature:home")
include(":feature:security")
include(":feature:clean")
include(":feature:history")
include(":feature:settings")
include(":feature:premium")
include(":feature:notifications")
include(":feature:realtime")

// Perf + test infra
include(":benchmark")
