package com.space.antivirus.domain.repository

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.EnumerationFilter
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.core.model.ScanTarget

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

    /** Resolves every scope in a ScanRequest into concrete ScanTargets,
     *  combining file and application enumeration as needed. This is the
     *  one method most future callers (a future scan-orchestration
     *  UseCase) actually need — enumerateInstalledApplications/
     *  enumerateFiles exist mainly so this method has something to
     *  compose, and so each capability is independently testable. */
    suspend fun resolveScanTargets(request: ScanRequest): AppResult<List<ScanTarget>>
}
