package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.TrustedItemRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

class RemoveTrustedItemUseCase @Inject constructor(
    private val repository: TrustedItemRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<String, Unit>(dispatcher) {

    override suspend fun execute(params: String): AppResult<Unit> =
        repository.removeTrustedItem(params)
}
