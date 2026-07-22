package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.domain.repository.SecurityRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.domain.UseCase

/**
 * Creates a new scan session and immediately transitions it to RUNNING —
 * the two repository calls are coordinated here (not left to the caller)
 * so a ViewModel can't accidentally create a session and forget to start
 * it, per Sprint 002 §7's rule that multi-repository-call coordination
 * belongs in a UseCase.
 */
class StartScanSessionUseCase @Inject constructor(
    private val repository: SecurityRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<ScanType, ScanSession>(dispatcher) {

    override suspend fun execute(params: ScanType): AppResult<ScanSession> {
        val created = repository.createScanSession(params)
        if (created !is AppResult.Success) return created
        return repository.startScanSession(created.data.id)
    }
}
