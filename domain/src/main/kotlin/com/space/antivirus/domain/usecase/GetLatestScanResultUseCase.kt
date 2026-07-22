package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.domain.NoParamsUseCase
import com.space.antivirus.domain.repository.SecurityRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

class GetLatestScanResultUseCase @Inject constructor(
    private val repository: SecurityRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : NoParamsUseCase<ScanResult?>(dispatcher) {

    override suspend fun execute(): AppResult<ScanResult?> =
        repository.getLatestScanResult()
}
