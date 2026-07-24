package com.space.antivirus.core.model

/** What kind of identifier a TrustedItem refers to — a file/folder path
 *  or an installed application's package name. Mirrors the same
 *  file-vs-application split ScanTarget and AnalyzerCapability already
 *  use, deliberately kept consistent rather than inventing a parallel
 *  vocabulary. */
enum class TrustedItemType {
    FILE,
    APPLICATION,
}
