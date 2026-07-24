# ADR 0024: SecurityRepositoryImpl — closing ADR 0014's long-deferred item

**Status:** Accepted

## Context
`SecurityRepository` has been contract-only since Sprint 004A. ADR 0014 (Sprint 004B) named the exact pattern to follow when it finally got a real implementation: its own module, Room-backed, `@Binds`-based binding, matching `core:enumeration`'s precedent. Sprint 010 (ADR 0023) deliberately built only the entities and DAOs first, splitting off the higher-risk mapping/orchestration layer for separate review. This sprint is that layer.

## Decisions

### 1. New module: `core:securitydata`
Not `core:security` (that name is already taken — Sprint 003's encryption/Keystore module) and not `core:securityengine` (ADR 0014's tentative suggestion, but that name risks confusion with a future actual detection engine). `core:securitydata` names what it actually is: the data/persistence implementation for `SecurityRepository`.

### 2. `completeScanSession` uses `AppDatabase.withTransaction`
The only method touching multiple tables together (session + statistics + threats + detections). Without a transaction, a process death mid-write could leave a session marked `COMPLETED` with no statistics row — a real data-integrity gap, not a cosmetic one. Every other multi-row-affecting method (`deleteScanHistory`, `clearScanHistory`) relies on SQLite's own CASCADE foreign keys (ADR 0023), which are already atomic as a single DELETE statement — no separate transaction needed there.

### 3. `ScanProgress` stored in a `ConcurrentHashMap`, not a plain map
Per ADR 0023, `ScanProgress` stays in-memory. `SecurityRepositoryImpl` is a real Hilt `@Singleton` running in a genuinely multi-threaded app — unlike `FakeSecurityRepository`'s single-threaded test double, concurrent `getOrPut` calls from multiple scan-adjacent coroutines against a plain `mutableMapOf` would be a real, if intermittent, bug. `ConcurrentHashMap` costs nothing extra here and removes the risk entirely.

### 4. Every method wrapped in a shared `safeCall` helper — with the same `CancellationException` discipline as `AnalyzerExecutor` (ADR 0019)
`CancellationException` is caught and immediately rethrown before the general `catch (e: Exception)` — it extends `Exception`, so a naive catch would silently swallow structured-concurrency cancellation of whatever coroutine called into this repository. This is the second time this exact discipline has been needed in this project; both places now cite each other.

### 5. `observeScanHistory`'s Flow correctness depends on an invariant, documented explicitly in code
Room's Flow invalidation tracks the table a query reads — `observeScanHistory()` queries `scan_sessions` only, then separately fetches statistics/threats per session inside the `.map` transform. This only stays correct because `completeScanSession`'s transaction always writes to `scan_sessions` in the same transaction as any statistics/threats change. If a future change ever wrote statistics/threats independently of their owning session row, this Flow would silently go stale (not re-emit) without any error. Documented directly in `SecurityRepositoryImpl`'s KDoc on that method, not just here, so it's visible at the point future maintainers are most likely to break it.

### 6. Real end-to-end tests, not mocked DAOs
`SecurityRepositoryImplTest` uses a real in-memory Room database, not mocked `ScanSessionDao`/etc. via mockk. `AppDatabase.withTransaction` is a Kotlin extension function; reliably mocking an extension function call on a mock receiver requires `mockkStatic` on the generated file class — fragile and easy to get subtly wrong in a way that looks like it works but doesn't actually verify transaction behavior. A real in-memory database exercises the actual transaction, cascade-delete, and Flow-invalidation behavior this repository depends on, at the cost of needing a real Android runtime (hence: instrumented test, not JVM unit test — same as Sprint 010's DAO tests).

## Consequences
- `core:data`'s existing `DataModule.kt` gained four new `@Provides` methods (one per DAO) — the natural, minimal extension of its existing role providing `AppDatabase`, rather than duplicating that construction knowledge inside `core:securitydata`.
- `:app` now depends on `core:securitydata`, following the same Hilt-module-discovery requirement every previous repository-implementation module (`core:enumeration`, Sprint 004B) already established.
- This closes ADR 0014's explicitly-named gap. `SecurityRepository` now has exactly one production implementation, following the exact pattern that ADR described three sprints in advance of it actually happening.
- No `domain` files were touched this sprint — every consumer of `SecurityRepository` (all the `RunScanRequestUseCase`-adjacent UseCases, `FakeSecurityRepository` in tests) is completely unaffected, satisfying backward compatibility by construction rather than by extra verification effort.
