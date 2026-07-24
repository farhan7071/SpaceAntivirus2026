package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.TrustedItem
import com.space.antivirus.core.model.TrustedItemType
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.TrustedItemRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

data class AddTrustedItemParams(
    val identifier: String,
    val type: TrustedItemType,
    val reason: String? = null,
)

class AddTrustedItemUseCase @Inject constructor(
    private val repository: TrustedItemRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<AddTrustedItemParams, TrustedItem>(dispatcher) {

    override suspend fun execute(params: AddTrustedItemParams): AppResult<TrustedItem> =
        repository.addTrustedItem(params.identifier, params.type, params.reason)
}
