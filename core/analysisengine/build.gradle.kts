plugins {
    id("spaceav.android.library")
    id("spaceav.android.hilt")
}

android {
    namespace = "com.space.antivirus.core.analysisengine"
}

dependencies {
    // BUGFIX-avoidance, same lesson as Sprint 003.5 (core:data needed
    // Room directly, not just transitively through core:database's
    // implementation-scoped dependency): :domain declares core:model and
    // core:common as `implementation`, not `api`, so a module depending
    // only on :domain does not automatically see those types on its own
    // compile classpath. This module's binding declarations resolve
    // ThreatAnalyzer/ThreatAnalyzerRegistry's signatures, which reference
    // core:model types (ScanTarget, AnalysisOutcome) even without
    // importing them directly — declared explicitly here rather than
    // relying on an implicit transitive path that doesn't actually exist.
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":domain"))
}
