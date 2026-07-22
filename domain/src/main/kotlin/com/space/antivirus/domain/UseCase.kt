package com.space.antivirus.domain

import com.space.antivirus.core.common.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Base type for every use case (Sprint 002 §7: "UseCase coordinates
 * multiple Repository calls with business logic between them — that
 * coordination belongs in a UseCase, not a ViewModel or a Repository").
 * No concrete use cases exist yet in Sprint 003 — "no business logic"
 * per the sprint's strict rules — but the base class and its test
 * establish the pattern every feature module will follow from Sprint 004.
 */
abstract class UseCase<in Params, out Result>(
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(params: Params): AppResult<Result> =
        withContext(dispatcher) {
            execute(params)
        }

    protected abstract suspend fun execute(params: Params): AppResult<Result>
}

/** For use cases that take no parameters. */
abstract class NoParamsUseCase<out Result>(
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): AppResult<Result> =
        withContext(dispatcher) {
            execute()
        }

    protected abstract suspend fun execute(): AppResult<Result>
}
