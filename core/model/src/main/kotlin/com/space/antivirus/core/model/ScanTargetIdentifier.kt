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

/**
 * Sprint 029 — the human-readable label for a target, distinct from
 * `identifier` (a stable, machine-oriented key). Added for a real,
 * verified root cause found while investigating a real-device report:
 * Threat never carried the app's display name at all — only
 * targetIdentifier (a package name). SecurityCenterScreen's ThreatCard
 * showed threat.title (a generic, threatType-derived category label like
 * "Unusual permission combination") as its headline instead of the app's
 * actual name. When different apps triggered the same threatType, they
 * all showed IDENTICAL headline text, indistinguishable from literal
 * duplication in a list — this is what real-device testers experienced
 * as "duplicate findings," even though the underlying architecture
 * already guarantees one Threat per app (verified directly: domain
 * aggregation, Room persistence, and repository reconstruction were all
 * already correct). See ADR 0043.
 *
 * FileTarget has no "app label" concept — falls back to its own
 * identifier (the file path) rather than throwing or returning a
 * placeholder string, since no file-based analyzer exists yet to
 * exercise this branch in practice.
 */
val ScanTarget.displayLabel: String
    get() = when (this) {
        is ScanTarget.FileTarget -> metadata.path
        is ScanTarget.ApplicationTarget -> application.appLabel
    }
