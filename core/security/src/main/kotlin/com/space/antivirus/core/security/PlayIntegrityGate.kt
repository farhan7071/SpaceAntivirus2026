package com.space.antivirus.core.security

/**
 * Abstraction boundary for Play Integrity API checks (e.g. gating
 * premium-entitlement trust decisions). Kept as an interface so
 * core:security has zero direct Play Integrity SDK dependency until a
 * feature module actually needs it — avoids pulling in a dependency this
 * sprint can't verify resolves (see README "Known Limitations").
 */
interface PlayIntegrityGate {
    suspend fun requestIntegrityVerdict(): IntegrityVerdict
}

sealed interface IntegrityVerdict {
    data object Trusted : IntegrityVerdict
    data object Unverified : IntegrityVerdict
    data class Failed(val reason: String) : IntegrityVerdict
}
