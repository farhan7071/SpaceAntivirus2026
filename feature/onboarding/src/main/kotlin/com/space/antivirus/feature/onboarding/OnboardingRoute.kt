package com.space.antivirus.feature.onboarding

/**
 * Navigation route constant for the Onboarding destination. Kept as a plain
 * const rather than a type-safe Navigation-Compose object for Sprint 003
 * (AndroidX Navigation's Kotlin-serialization-based type safety needs the
 * BOM version this sandbox cannot verify resolves — see README). Migrating
 * to type-safe routes is a safe, isolated Sprint 004 follow-up per
 * docs/adr/0009-navigation-routes.md.
 */
const val OnboardingNavigationRoute = "onboarding"
