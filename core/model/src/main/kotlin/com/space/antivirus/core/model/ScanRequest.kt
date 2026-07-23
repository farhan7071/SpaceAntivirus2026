package com.space.antivirus.core.model

/**
 * What the user (or a schedule, in a later sprint) asked to be scanned —
 * the input to enumeration, before it's been resolved into concrete
 * ScanTargets. Intentionally decoupled from ScanSession (Sprint 004A):
 * a ScanRequest describes intent ("scan the Downloads folder"); a
 * ScanSession tracks the lifecycle of actually doing it. The same
 * ScanRequest shape will feed whatever creates a ScanSession in a later
 * sprint, once enumeration and detection are wired together.
 */
data class ScanRequest(
    val id: String,
    val scanType: ScanType,
    val scopes: List<ScanScope>,
    val filter: EnumerationFilter = EnumerationFilter.DEFAULT,
    val createdAtEpochMillis: Long,
) {
    init {
        require(scopes.isNotEmpty()) { "A ScanRequest must specify at least one ScanScope" }
    }
}
