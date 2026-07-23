package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.InstalledApplicationInfo
import com.space.antivirus.domain.NoParamsUseCase
import com.space.antivirus.domain.repository.EnumerationRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

class EnumerateInstalledApplicationsUseCase @Inject constructor(
    private val repository: EnumerationRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : NoParamsUseCase<List<InstalledApplicationInfo>>(dispatcher) {

    override suspend fun execute(): AppResult<List<InstalledApplicationInfo>> =
        repository.enumerateInstalledApplications()
}
