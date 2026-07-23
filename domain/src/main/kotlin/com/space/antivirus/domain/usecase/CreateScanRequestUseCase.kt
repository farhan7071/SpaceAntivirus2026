package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.EnumerationFilter
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.core.model.ScanType
import java.util.UUID
import javax.inject.Inject

/**
 * Builds a well-formed ScanRequest (generated id + timestamp) from what a
 * future ViewModel would collect from the user (scan type + chosen
 * scopes). Not built on the UseCase base class — this is synchronous,
 * pure construction with no repository call and nothing that can fail in
 * a way AppResult would need to represent beyond what ScanRequest's own
 * `init` block already validates (empty scopes list), so it throws that
 * validation exception directly rather than wrapping a guaranteed-success
 * path in AppResult for no reason.
 */
class CreateScanRequestUseCase @Inject constructor() {

    operator fun invoke(
        scanType: ScanType,
        scopes: List<ScanScope>,
        filter: EnumerationFilter = EnumerationFilter.DEFAULT,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): ScanRequest =
        ScanRequest(
            id = UUID.randomUUID().toString(),
            scanType = scanType,
            scopes = scopes,
            filter = filter,
            createdAtEpochMillis = nowEpochMillis,
        )
}
