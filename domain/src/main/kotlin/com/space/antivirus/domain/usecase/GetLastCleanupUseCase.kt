package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.CleanupRecord
import com.space.antivirus.domain.NoParamsUseCase
import com.space.antivirus.domain.repository.CleanupHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The most recent cleanup, or null if the user has never run one — what
 * the Cleaner's "Last cleanup" line reads. Sprint 038 omitted that line
 * because nothing persisted it; this is what fills it.
 */
class GetLastCleanupUseCase @Inject constructor(
    private val repository: CleanupHistoryRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : NoParamsUseCase<CleanupRecord?>(dispatcher) {

    override suspend fun execute(): AppResult<CleanupRecord?> = repository.latest()
}
