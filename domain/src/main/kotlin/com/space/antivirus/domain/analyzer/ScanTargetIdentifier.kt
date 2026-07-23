package com.space.antivirus.domain.analyzer

import com.space.antivirus.core.model.ScanTarget

/**
 * The one place ScanTarget's variant-specific identifier (a file path for
 * FileTarget, a package name for ApplicationTarget) is unified into a
 * single String — needed anywhere code has to refer to "this target" in a
 * variant-agnostic way, e.g. building an AnalysisOutcome. Same reasoning
 * as ScanTargetCapability.kt: small and pure, but centralized so it's
 * defined once, not reimplemented at each call site.
 */
val ScanTarget.identifier: String
    get() = when (this) {
        is ScanTarget.FileTarget -> metadata.path
        is ScanTarget.ApplicationTarget -> application.packageName
    }
