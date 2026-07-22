package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.Threat
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.SecurityRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

data class CompleteScanSessionParams(
    val sessionId: String,
    val statistics: ScanStatistics,
    val threats: List<Threat>,
)

class CompleteScanSessionUseCase @Inject constructor(
    private val repository: SecurityRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<CompleteScanSessionParams, ScanResult>(dispatcher) {

    override suspend fun execute(params: CompleteScanSessionParams): AppResult<ScanResult> =
        repository.completeScanSession(params.sessionId, params.statistics, params.threats)
}
