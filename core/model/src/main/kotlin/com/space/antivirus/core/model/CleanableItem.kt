package com.space.antivirus.core.model

/**
 * A single file identified as reclaimable storage. Deliberately carries
 * no "confidence" or "risk" field — this is a storage-reclamation
 * classification, not a security verdict (see CleanableCategory's own
 * KDoc). `reason` exists for the same evidence-first principle
 * established for Detection (Sprint 004C, Sprint 002.75 §17) — a user
 * should never see "this is junk" without being told why.
 *
 * This models a CANDIDATE for cleanup, not an action taken — nothing in
 * this project yet deletes a file. Deletion is explicitly a future Clean
 * UI sprint's concern, once this domain layer exists for it to act on.
 */
data class CleanableItem(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val category: CleanableCategory,
    val reason: String,
) {
    init {
        require(path.isNotBlank()) { "path cannot be blank" }
        require(name.isNotBlank()) { "name cannot be blank" }
        require(sizeBytes >= 0) { "sizeBytes cannot be negative" }
        require(reason.isNotBlank()) { "reason cannot be blank" }
    }
}
