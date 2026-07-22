package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.SecurityRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

class GetScanResultUseCase @Inject constructor(
    private val repository: SecurityRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<String, ScanResult>(dispatcher) {

    override suspend fun execute(params: String): AppResult<ScanResult> =
        repository.getScanResult(params)
}
