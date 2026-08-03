package com.space.antivirus.domain.repository

import com.space.antivirus.core.common.AppResult

/**
 * The only way anything in this project deletes a file — Sprint 039.
 *
 * Until this sprint, nothing did. That was deliberate (ADR 0035): the
 * junk domain layer identified candidates and stopped there, and Sprint
 * 038 built a Cleaner UI that said so plainly rather than shipping a
 * button that lied. This interface is the capability those two sprints
 * deferred.
 *
 * **Containment is part of the contract, not an implementation detail.**
 * An implementation MUST refuse to delete anything outside the app's own
 * private storage, and MUST refuse regardless of what any caller asks
 * for. A cleaner that can be handed an arbitrary path and will delete it
 * is one bug away from destroying a user's photos; the guard therefore
 * lives at the lowest possible level, below every use case, so no
 * calling code can forget it or opt out. See ADR 0054 for what the app
 * can and cannot legally reach on API 30+, and why that boundary is
 * where it is.
 *
 * Deletion failures are ordinary, expected outcomes — a file can vanish
 * between being scanned and being deleted, or be held open. They are
 * returned as AppResult.Failure per item, never thrown, so a cleanup can
 * count them and keep going rather than aborting a batch because one
 * file was stubborn.
 */
interface FileDeletionRepository {

    /**
     * Deletes one file and reports how many bytes that actually freed.
     *
     * Returns the file's real size, measured immediately before deletion
     * — not the size recorded when it was scanned, which may be stale.
     * Returns `AppResult.Success(0)` for a file that no longer exists:
     * the desired end state is already true, and the caller freed
     * nothing, both of which are honest.
     *
     * Fails with [com.space.antivirus.core.common.AppError.PermissionMissing]
     * if the path lies outside app-private storage — the containment
     * guard above — and with StorageUnavailable if the delete itself is
     * refused by the filesystem.
     */
    suspend fun deleteFile(path: String): AppResult<Long>

    /** True if [path] is inside a directory this app is allowed to
     *  delete from. Exposed so callers can filter a candidate list up
     *  front instead of discovering the boundary one failure at a time. */
    fun isDeletable(path: String): Boolean
}
