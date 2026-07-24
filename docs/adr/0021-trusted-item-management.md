# ADR 0021: Trusted Item management

**Status:** Accepted

## Context
Sprint 002.5's UX specification named a "Trusted List" screen as part of the Security Center information architecture back near the start of this project, and `EnumerationFilter` (Sprint 004B) already has an `excludedPathPrefixes` field — but nothing has ever populated that list from a persistent, user-managed source. There was no repository, no model, and no way for a user's "stop scanning this" decision to survive between sessions.

No explicit Sprint 008 brief was given; this was chosen as the most clearly-justified, narrowly-scoped next capability — a real, previously-named gap, requiring no new product or detection-strategy decisions, and containable entirely within `core:model` and `domain`.

## Decision
1. **`TrustedItem`** (core:model): `id`, `identifier`, `type` (`FILE`/`APPLICATION`, mirroring the same split `ScanTarget`/`AnalyzerCapability` already use), `addedAtEpochMillis`, optional `reason`. Deliberately carries no risk/severity field — trusting an item is user consent to stop checking it, not a claim that it was ever verified safe. Conflating the two would misrepresent what "trusted" means.
2. **`TrustedItemRepository`** (domain): contract only, no implementation — same discipline as `SecurityRepository` (004A) and `EnumerationRepository` (004B) at their introduction. `addTrustedItem` is specified as idempotent by `(identifier, type)`: adding an already-trusted identifier under the same type returns the existing item rather than creating a duplicate row. This is a real behavioral requirement on the interface, not just the fake's convenience — any future implementation must honor it, or the trusted list could silently accumulate redundant entries every time a user re-adds something already on it.
3. Four UseCases (`AddTrustedItemUseCase`, `RemoveTrustedItemUseCase`, `IsTrustedUseCase`, `ObserveTrustedItemsUseCase`), matching this project's established thin-wrapper-around-one-repository-call pattern, with the Flow-returning one exposing `Flow` directly per the precedent set by `ObserveScanHistoryUseCase`/`ObserveScanProgressUseCase`.
4. **Deliberately NOT wired into `RunScanRequestUseCase`/`ResolveScanTargetsUseCase` this sprint.** `IsTrustedUseCase` exists specifically so that future wiring is a call to an already-tested UseCase, not new logic invented under time pressure later — but actually skipping trusted targets during a scan, and deciding how that should be reflected in `ScanStatistics` (a third category, distinct from both `threatsFound` and `itemsInconclusive`?), is a real design question deserving its own focused sprint rather than being folded into "add trusted-item management" as an afterthought.

## Consequences
- The eventual Trusted List UI (whenever a UI sprint reaches it) has real, tested domain logic to build against today.
- The scan pipeline does not yet respect the trusted list — a trusted item is still fully scanned and can still be flagged. This is a known, explicitly-scoped gap, not an oversight; wiring it in is named as the next natural step, not silently deferred without acknowledgment.
- `AppError.TrustedItemNotFound` extends the existing closed `AppError` set (ADR 0007/0013/0020) rather than introducing a parallel error type, consistent with every prior extension.
- When the scan-pipeline wiring does happen, it will need to decide how a skipped-because-trusted target is counted — extending `ScanStatistics` a third time (after Sprint 005's `itemsInconclusive`) is the most likely shape, but that's a decision for whichever sprint actually does the wiring, not this one.
