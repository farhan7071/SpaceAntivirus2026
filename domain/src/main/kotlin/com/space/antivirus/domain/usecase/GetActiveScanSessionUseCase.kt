package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.domain.NoParamsUseCase
import com.space.antivirus.domain.repository.SecurityRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Whether a scan is currently running — the check a future "Scan now"
 * button would make before offering to start another (e.g. to disable
 * itself or show "a scan is already in progress" instead). Success(null)
 * means nothing is currently active, not an error.
 */
class GetActiveScanSessionUseCase @Inject constructor(
    private val repository: SecurityRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : NoParamsUseCase<ScanSession?>(dispatcher) {

    override suspend fun execute(): AppResult<ScanSession?> = repository.getActiveScanSession()
}
