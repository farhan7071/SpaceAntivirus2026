package com.space.antivirus.core.model

/**
 * A file path or application the user has explicitly chosen to exclude
 * from future scans — the domain backing for the "Trusted List" screen
 * named in Sprint 002.5's UX spec (§5 information architecture) but never
 * given real data until now.
 *
 * Deliberately does NOT carry any risk/severity field — trusting an item
 * is a user decision to stop checking it, not a claim that it was ever
 * verified safe. Conflating the two would misrepresent what "trusted"
 * means here: it's consent, not a verdict.
 */
data class TrustedItem(
    val id: String,
    val identifier: String,
    val type: TrustedItemType,
    val addedAtEpochMillis: Long,
    val reason: String? = null,
) {
    init {
        require(identifier.isNotBlank()) { "identifier cannot be blank" }
    }
}
