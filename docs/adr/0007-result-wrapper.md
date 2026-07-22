# ADR 0007: Sealed AppResult/AppError instead of exceptions across architectural boundaries

**Status:** Accepted

## Context
Sprint 002 §7 mandated 'sealed Result/UiState types only, no bare error string.' Sprint 002.75 §8 requires every user-facing error to map to a known category with title/description/recovery copy.

## Decision
core:common defines a single AppResult<T> sealed interface (Success/Failure/Loading) and a closed AppError sealed interface (Network/PermissionMissing/StorageUnavailable/EngineUnavailable/Unexpected) used at every Repository -> UseCase -> ViewModel boundary.

## Consequences
Every feature's error UI can switch exhaustively over AppError without an else branch (compiler-enforced completeness) — matches the copy table in Sprint 002.75 §8 one-to-one. Tradeoff: repositories must translate every underlying exception (network, Room, engine) into one of these categories rather than letting it propagate raw, adding a small amount of boilerplate at each data-source boundary.
