package com.space.antivirus.domain.repository

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.EnumerationFilter
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.core.model.ScanTarget
import kotlinx.coroutines.flow.Flow

/**
 * Contract for answering "what can be scanned" — enumeration only, never
 * detection. Nothing in this interface returns a verdict, a hash, or a
 * risk level; it returns inventories. The implementation (core:enumeration,
 * this same sprint's PATCH 2) is the only place Android's PackageManager
 * or filesystem APIs are touched — UseCases and anything above them never
 * see those types directly, same discipline as SecurityRepository
 * (Sprint 004A).
 *
 * Failure reasons reuse the existing AppError (ADR 0007): a missing
 * storage/package-visibility permission maps to AppError.PermissionMissing,
 * an inaccessible path maps to AppError.StorageUnavailable, and an
 * unresolvable user-selected folder maps to AppError.InvalidScanConfiguration
 * — no new AppError cases were needed for this sprint, unlike Sprint 004A.
 */
interface EnumerationRepository {

    /** All installed applications currently on the device. */
    suspend fun enumerateInstalledApplications(): AppResult<List<InstalledApplicationInfo>>

    /** Every file/directory reachable under the given scope, after
     *  `filter` is applied. `scope` must be a file-oriented ScanScope
     *  (not InstalledApplications) — see the implementation's contract
     *  test for the exact failure behavior if it isn't. */
    suspend fun enumerateFiles(
        scope: ScanScope,
        filter: EnumerationFilter = EnumerationFilter.DEFAULT,
    ): AppResult<List<FileMetadata>>

    /**
     * Whether [scope] resolves to a location that currently exists and
     * can be read — Sprint 039.
     *
     * Cheap: resolves the root and stats it, never walks it. Exists
     * because [enumerateFilesAsFlow] cannot report a root-resolution
     * failure per element, and a streaming scan that silently completed
     * empty on an unreadable volume would tell the user their storage
     * was clean when it had in fact never been looked at. A caller that
     * streams should check this first.
     */
    suspend fun isScopeAvailable(scope: ScanScope): AppResult<Unit>

    /**
     * The same enumeration as [enumerateFiles], streamed one file at a
     * time as the walk visits it — Sprint 039.
     *
     * Added so callers that need to report genuine progress can, without
     * waiting for the entire tree to be walked first. Emits nothing and
     * completes normally if the scope cannot be resolved; callers that
     * need to distinguish "empty" from "unavailable" should call
     * [enumerateFiles], which still returns a real AppResult.Failure.
     * That is a deliberate split rather than wrapping every element in a
     * result type: a per-file failure channel would be noise, since the
     * only failure mode here is resolving the root, which happens once.
     */
    fun enumerateFilesAsFlow(
        scope: ScanScope,
        filter: EnumerationFilter = EnumerationFilter.DEFAULT,
    ): Flow<FileMetadata>

    /** Resolves every scope in a ScanRequest into concrete ScanTargets,
     *  combining file and application enumeration as needed. This is the
     *  one method most future callers (a future scan-orchestration
     *  UseCase) actually need — enumerateInstalledApplications/
     *  enumerateFiles exist mainly so this method has something to
     *  compose, and so each capability is independently testable. */
    suspend fun resolveScanTargets(request: ScanRequest): AppResult<List<ScanTarget>>
}
