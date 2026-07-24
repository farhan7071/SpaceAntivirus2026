package com.space.antivirus.domain.usecase

import com.space.antivirus.core.model.TrustedItem
import com.space.antivirus.domain.repository.TrustedItemRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Same reasoning as ObserveScanHistoryUseCase/ObserveScanProgressUseCase:
 *  a live-updating list is a Flow, not a one-shot AppResult. */
class ObserveTrustedItemsUseCase @Inject constructor(
    private val repository: TrustedItemRepository,
) {
    operator fun invoke(): Flow<List<TrustedItem>> = repository.observeTrustedItems()
}
