package com.space.antivirus.domain.usecase

import com.space.antivirus.core.model.ScanProgress
import com.space.antivirus.domain.repository.SecurityRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Same reasoning as ObserveScanHistoryUseCase (Sprint 004A): live
 * progress is a Flow, not a one-shot AppResult, so it's exposed directly
 * rather than forced through the UseCase base class.
 */
class ObserveScanProgressUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    operator fun invoke(sessionId: String): Flow<ScanProgress> = repository.observeScanProgress(sessionId)
}
