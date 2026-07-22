# ADR 0013: Security-domain error cases extend the existing AppError, plus a dependency-graph fix found along the way

**Status:** Accepted

## Context
Sprint 004A's brief calls for an "error hierarchy" as one of the domain
layer's deliverables. The project already has one: `AppResult`/`AppError`
in `core:common`, established by ADR 0007 specifically so every
architectural boundary uses one sealed, exhaustively-checkable error type
instead of raw exceptions. Introducing a second, parallel `SecurityError`
hierarchy alongside it would mean every caller eventually has to reconcile
two different failure-reporting mechanisms — exactly the kind of
duplicated responsibility Sprint 004A's own rules ("no shortcuts", SOLID)
warn against.

The `SecurityRepository` contract added in this sprint needs two failure
reasons `AppError` didn't have: "no such scan session" and "the requested
operation isn't valid for this session's current state" (e.g. asking for
a `ScanResult` before the session has reached a terminal state).

Separately, while wiring the new `UseCase` implementations, I found that
`core:common`'s `@IoDispatcher`/`@DefaultDispatcher`/`@MainDispatcher`
Hilt qualifiers (declared in Sprint 003) were never bound to an actual
`CoroutineDispatcher` anywhere — they had zero consumers until this
sprint's `UseCase` classes injected `@IoDispatcher` directly, which is
what surfaced the gap. Without a binding, Hilt's compile-time graph
validation fails.

## Decision
1. Extend `AppError` with two new cases: `ScanSessionNotFound(sessionId)`
   and `InvalidScanConfiguration(reason)`, rather than creating a second
   error hierarchy. Confirmed no existing `when` expression exhaustively
   switches over `AppError` anywhere in the codebase yet, so this is a
   safe, non-breaking addition.
2. Added `core:data/.../di/DispatcherModule.kt`, a `@Module` binding all
   three dispatcher qualifiers to their real `kotlinx.coroutines.Dispatchers`
   equivalents. Placed in `core:data` because that's already the
   established home for infrastructure `@Module`s (`NetworkModule`,
   `DataModule`) and because `core:common` is a pure-Kotlin JVM module
   (ADR 0011) that can't host Hilt/Dagger annotation processing itself.
3. Removed the unused `AppDispatchers` wrapper class that Sprint 003 left
   in `core:common` alongside the qualifiers — it had no consumers and
   represented a second, competing way to access dispatchers. The
   qualifier-based direct-injection pattern (now actually used by the
   `UseCase` layer) is the one pattern going forward.

## Consequences
- `SecurityRepository`'s contract can express "not found" and "invalid
  state" failures through the same `AppResult<T>` every other boundary in
  the app already uses — a `ViewModel` handling a `SecurityRepository`
  failure and a `ViewModel` handling a network failure use identical
  exhaustive-`when` logic, just different cases.
- Any future domain needing new failure reasons should default to adding
  a case to `AppError` (as done here) rather than inventing a parallel
  hierarchy, unless a genuinely different transport mechanism is
  involved — this ADR sets that precedent explicitly.
- The dispatcher-binding gap is now closed for real, not just for this
  sprint's use cases — any future `@Inject`-constructor class anywhere in
  the app can request `@IoDispatcher`/`@DefaultDispatcher`/`@MainDispatcher`
  and Hilt will satisfy it.
- This is the third time in this project a Sprint-003-era declaration
  turned out to have no real binding/consumer to validate it against
  (after the `com.android.library`/`application` conflict and the missing
  Room/material-icons-extended dependencies) — worth treating as a
  standing pattern: code with zero consumers is unverified by
  construction, regardless of how deliberate it looked when written.
