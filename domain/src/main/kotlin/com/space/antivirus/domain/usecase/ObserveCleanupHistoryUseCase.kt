package com.space.antivirus.domain.usecase

import com.space.antivirus.core.model.CleanupRecord
import com.space.antivirus.domain.repository.CleanupHistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Same reasoning as ObserveScanHistoryUseCase (Sprint 004A): a live list
 * is a Flow, not a one-shot AppResult, so it's exposed directly rather
 * than forced through the UseCase base class.
 */
class ObserveCleanupHistoryUseCase @Inject constructor(
    private val repository: CleanupHistoryRepository,
) {
    operator fun invoke(): Flow<List<CleanupRecord>> = repository.observeHistory()
}
