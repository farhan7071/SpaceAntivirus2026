package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.StorageStatistics
import com.space.antivirus.domain.NoParamsUseCase
import com.space.antivirus.domain.repository.StorageStatisticsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/** Device storage totals for the Cleaner's storage line — Sprint 039. */
class GetStorageStatisticsUseCase @Inject constructor(
    private val repository: StorageStatisticsRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : NoParamsUseCase<StorageStatistics>(dispatcher) {

    override suspend fun execute(): AppResult<StorageStatistics> = repository.getStorageStatistics()
}
