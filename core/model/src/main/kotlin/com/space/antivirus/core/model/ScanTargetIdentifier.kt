package com.space.antivirus.core.model

/**
 * The one place ScanTarget's variant-specific identifier (a file path for
 * FileTarget, a package name for ApplicationTarget) is unified into a
 * single String — needed anywhere code has to refer to "this target" in a
 * variant-agnostic way, e.g. building an AnalysisOutcome. Centralized here
 * in core:model (Sprint 027 fix) so it's guaranteed visible to every
 * module using ScanTarget, avoiding cross-module extension visibility
 * issues seen when it was in :domain.
 */
val ScanTarget.identifier: String
    get() = when (this) {
        is ScanTarget.FileTarget -> metadata.path
        is ScanTarget.ApplicationTarget -> application.packageName
    }
