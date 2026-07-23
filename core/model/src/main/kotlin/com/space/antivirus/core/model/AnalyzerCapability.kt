package com.space.antivirus.core.model

/**
 * What kind of ScanTarget an analyzer can process. Lets a future
 * ThreatAnalyzerRegistry route each ScanTarget only to analyzers that can
 * actually handle it — a signature engine over file contents has nothing
 * useful to say about an InstalledApplicationInfo, and vice versa for a
 * permission-heuristic engine.
 */
enum class AnalyzerCapability {
    FILE_ANALYSIS,
    APPLICATION_ANALYSIS,
}
