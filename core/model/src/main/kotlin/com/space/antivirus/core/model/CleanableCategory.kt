package com.space.antivirus.core.model

/**
 * Category of a reclaimable-storage finding. Deliberately a small,
 * generic, evidence-describable set — same discipline as ThreatType
 * (Sprint 004C): this enum doesn't grow just because a new marketing
 * term for "junk" exists, and every classifier decision must be
 * justifiable against one of these categories, not an ad hoc label.
 *
 * Distinct from ThreatType on purpose — a cache file is not a security
 * concern, and conflating "reclaimable storage" with "threat" would
 * misrepresent what a Cleanable finding actually means to a user.
 */
enum class CleanableCategory {
    CACHE_FILE,
    TEMPORARY_FILE,
    LOG_FILE,
    LEFTOVER_INSTALLER,
}
