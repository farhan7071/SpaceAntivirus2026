package com.space.antivirus.domain.usecase

import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.domain.repository.SecurityRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Deliberately NOT built on the UseCase base class — UseCase wraps a
 * single suspend call in AppResult; a live-updating history list is a
 * Flow, not a one-shot result, and forcing it through AppResult would
 * mean collapsing "still loading" vs. "empty" vs. "error" into a single
 * emitted value awkwardly. Exposing the Flow directly is the standard
 * pattern for "Observe*" use cases in current Android architecture
 * guidance.
 */
class ObserveScanHistoryUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    operator fun invoke(): Flow<List<ScanResult>> = repository.observeScanHistory()
}
