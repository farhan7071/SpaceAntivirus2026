package com.space.antivirus.core.common

/**
 * Sealed result wrapper used at every Repository -> UseCase -> ViewModel
 * boundary in the app. Chosen over throwing exceptions across those
 * boundaries so ViewModels are forced to handle failure explicitly rather
 * than relying on try/catch discipline. See docs/adr/0007-result-wrapper.md
 * and Sprint 002 §7 ("sealed Result/UiState types only").
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
    data object Loading : AppResult<Nothing>
}

/**
 * Closed set of error categories. Deliberately NOT a raw Throwable — see
 * Sprint 002.75 §8 (Error Writing Guide): every user-facing error must map
 * to one of these categories so copy can be written once per category
 * instead of per-exception-type.
 *
 * ScanSessionNotFound and InvalidScanConfiguration were added in Sprint
 * 004A for the Security domain's Repository contract — see
 * docs/adr/0013-security-domain-error-cases.md. ScanAlreadyInProgress was
 * added in Sprint 007 — see docs/adr/0020-concurrent-scan-guarding.md.
 */
sealed interface AppError {
    data object Network : AppError
    data object PermissionMissing : AppError
    data object StorageUnavailable : AppError
    data object EngineUnavailable : AppError
    data class ScanSessionNotFound(val sessionId: String) : AppError
    data class InvalidScanConfiguration(val reason: String) : AppError
    data class ScanAlreadyInProgress(val activeSessionId: String) : AppError
    data class Unexpected(val cause: Throwable? = null) : AppError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
    is AppResult.Loading -> AppResult.Loading
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}
