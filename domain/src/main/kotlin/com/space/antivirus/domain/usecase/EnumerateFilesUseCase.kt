package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.EnumerationFilter
import com.space.antivirus.core.model.FileMetadata
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.EnumerationRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

data class EnumerateFilesParams(
    val scope: ScanScope,
    val filter: EnumerationFilter = EnumerationFilter.DEFAULT,
)

class EnumerateFilesUseCase @Inject constructor(
    private val repository: EnumerationRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<EnumerateFilesParams, List<FileMetadata>>(dispatcher) {

    override suspend fun execute(params: EnumerateFilesParams): AppResult<List<FileMetadata>> =
        repository.enumerateFiles(params.scope, params.filter)
}
