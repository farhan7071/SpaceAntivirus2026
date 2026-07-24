package com.space.antivirus.domain.repository

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.TrustedItem
import com.space.antivirus.core.model.TrustedItemType
import kotlinx.coroutines.flow.Flow

/**
 * Contract for the user-managed "Trusted List" — items explicitly
 * excluded from future scans (Sprint 002.5's UX spec named this screen;
 * this is its first real domain backing). Not yet wired into
 * RunScanRequestUseCase/ResolveScanTargetsUseCase — that integration is
 * deliberately deferred to a future sprint (see ADR 0021's consequences)
 * so this sprint stays scoped to trusted-item management as its own
 * complete, testable capability.
 */
interface TrustedItemRepository {

    /**
     * Adds a new trusted item. Idempotent by (identifier, type): adding
     * an identifier that's already trusted under the same type returns
     * the existing TrustedItem rather than creating a duplicate entry —
     * implementations must honor this so the trusted list can't silently
     * accumulate redundant rows for the same path/package.
     */
    suspend fun addTrustedItem(
        identifier: String,
        type: TrustedItemType,
        reason: String? = null,
    ): AppResult<TrustedItem>

    /** Removes a trusted item by id. Fails with
     *  AppError.TrustedItemNotFound if no such item exists. */
    suspend fun removeTrustedItem(id: String): AppResult<Unit>

    /** Whether the given identifier is currently on the trusted list,
     *  regardless of type. The check a future scan-orchestration UseCase
     *  would make per target before analyzing it. */
    suspend fun isTrusted(identifier: String): AppResult<Boolean>

    /** The full trusted list, most recently added first. A Flow (not a
     *  suspend function) so a future Trusted List screen updates live as
     *  items are added or removed. */
    fun observeTrustedItems(): Flow<List<TrustedItem>>
}
