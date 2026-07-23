package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.ScanRequest
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.EnumerationRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The one enumeration UseCase most future callers actually need — turns a
 * ScanRequest (intent) into the concrete list of ScanTargets a future
 * detection sprint would iterate over. A thin wrapper today (delegates
 * straight to the repository), but kept as its own UseCase rather than
 * inlined into a ViewModel because a later sprint is likely to add real
 * coordination here (e.g. deduplicating targets across overlapping
 * scopes) without that becoming a ViewModel's responsibility.
 */
class ResolveScanTargetsUseCase @Inject constructor(
    private val repository: EnumerationRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<ScanRequest, List<ScanTarget>>(dispatcher) {

    override suspend fun execute(params: ScanRequest): AppResult<List<ScanTarget>> =
        repository.resolveScanTargets(params)
}
