package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.repository.TrustedItemRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Not yet called anywhere in the scan pipeline (see TrustedItemRepository's
 * KDoc and ADR 0021) — exists now so that future wiring is a call to an
 * already-tested UseCase, not new logic invented under pressure later.
 */
class IsTrustedUseCase @Inject constructor(
    private val repository: TrustedItemRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<String, Boolean>(dispatcher) {

    override suspend fun execute(params: String): AppResult<Boolean> =
        repository.isTrusted(params)
}
