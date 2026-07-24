# ADR 0025: TrustedItemRepositoryImpl — the same treatment SecurityRepository got, in one sprint instead of two

**Status:** Accepted

## Context
`TrustedItemRepository` has been contract-only since Sprint 008 — the exact same situation `SecurityRepository` was in before Sprints 010/011 gave it a real implementation, split across two sprints specifically because Room's transaction/relational code was judged too risky to build and verify in one pass without a real compiler available. `AppDatabase`'s own KDoc (Sprint 010/ADR 0023) explicitly flagged `TrustedItemEntity` as "deliberately NOT here yet" — a direct pointer to this sprint's work.

No explicit Sprint 012 brief was given; this was the obvious next parallel gap, requiring no product decisions.

## Decision: one sprint, not two
Unlike `SecurityRepository`'s schema (four related tables, cascading foreign keys, a multi-table transaction in `completeScanSession`), `TrustedItem`'s schema is a single standalone table with no foreign keys and no operation that writes to more than one row atomically. The risk that justified splitting Sprint 010/011 — subtle mistakes in Room relational/transactional code that are hard to catch without a compiler — doesn't apply here at anywhere near the same scale. Building the entity, DAO, mapper, and repository implementation together in one sprint was a deliberate, evidence-based choice, not a departure from the caution that shaped Sprints 010/011 — the caution was proportionate to the actual complexity each time.

Concretely, this is why `core:trusteddata` (unlike `core:securitydata`) does **not** depend on Room directly: `TrustedItemRepositoryImpl` never calls a Room framework API itself (no `withTransaction`), only plain suspend functions on `TrustedItemDao` — there was nothing here that needed it.

## Other decisions, consistent with established precedent
- **Schema version 2 → 3**, same `fallbackToDestructiveMigration()` reasoning as version 1 → 2 (ADR 0023) — no real user data has ever existed in either schema state to preserve.
- **`deleteById` returns the affected row count (`Int`)** rather than requiring a separate existence check — a standard Room capability, used here specifically so `removeTrustedItem` can distinguish "removed" from "no such item" (`AppError.TrustedItemNotFound`) without an extra query.
- **Same `CancellationException`-caught-and-rethrown-first discipline** as `AnalyzerExecutor` (ADR 0019) and `SecurityRepositoryImpl` (ADR 0024) — the third time this exact pattern has been needed; all three now cite each other in their KDoc.
- **Idempotency enforced in the repository layer, not the DAO** — `findByIdentifierAndType` is a plain lookup; the "return existing rather than duplicate" rule (ADR 0021's original contract requirement) lives in `TrustedItemRepositoryImpl`, keeping the DAO itself a dumb, low-level layer, consistent with every DAO since Sprint 010.

## Consequences
- `TrustedItemRepository` now has exactly one production implementation. Combined with Sprint 011's `SecurityRepositoryImpl`, every repository this project has defined now has a real backing — `EnumerationRepository` (Sprint 004B), `SecurityRepository` (Sprint 011), `TrustedItemRepository` (this sprint).
- `IsTrustedUseCase` — added in Sprint 008 specifically so a future sprint wiring trust-checking into the scan pipeline (which Sprint 009 already did, against `FakeTrustedItemRepository`) would call an already-tested UseCase — now runs against real, persisted data for the first time. No `domain` files were touched to make this true; the wiring already existed and simply now has a real repository underneath it.
- No `domain` files were touched this sprint, satisfying backward compatibility by construction, same as Sprint 011.
