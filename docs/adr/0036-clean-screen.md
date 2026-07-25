# ADR 0036: Clean screen — genuinely closing Phase C

**Status:** Accepted

## Context
After Sprint 022, verification against current `main` found Phase C was not actually complete despite five of six roadmap items landing — `feature:clean` was still Sprint 003's literal placeholder, and nothing referenced `FindCleanableItemsUseCase` (Sprint 022) outside `domain/`. Confirmed directly with the user before proceeding, rather than assumed either way: build Clean UI now, genuinely closing Phase C, before Phase D begins.

## Decisions

### 1. Action-triggered ViewModel, not a Flow-combine one
Every other feature ViewModel in this project (`HomeViewModel`, `SecurityCenterViewModel`, `HistoryViewModel`) combines Flow-based UseCases reactively, because they all read from the same persisted, ongoing `ObserveScanHistoryUseCase` Flow. `FindCleanableItemsUseCase` is a one-shot file enumeration — there's no ongoing, observable data source to subscribe to. `CleanViewModel` follows `ScanViewModel`'s shape instead (Sprint 020): a plain `MutableStateFlow`, mutated by a user-triggered action. Same reasoning ADR 0033 already established for exactly this distinction.

### 2. Display-only — no selection, no deletion
`CleanableItem` (ADR 0035) was deliberately scoped as "candidates only... nothing in this domain layer deletes a file." No delete-capable UseCase or repository method exists anywhere in this project yet. Building a delete button that doesn't actually delete anything would be exactly the fake production code this project's standing rules prohibit. Real file deletion is real, separate domain work — permission handling, confirmation UX, irreversibility — deserving its own sprint, not something to fold into "add the Clean screen."

### 3. Fixed to `ScanScope.InternalStorage` for now
No scope-selection UI. `FindCleanableItemsUseCase` already takes `ScanScope` as a parameter specifically so a future sprint letting a user choose Downloads/external storage is a small, additive UI change — not a UseCase redesign.

### 4. Same double-trigger guard pattern as `ScanViewModel`, and its test-dispatcher gotcha applied proactively
The `_uiState.value is Loading` guard, and the `runCurrent()` fix for testing it under `StandardTestDispatcher`, were both applied directly from the start this time — ADR 0033 already documented the reasoning in full when this exact pattern first appeared in `ScanViewModel`, so there was nothing new to discover here.

## Consequences
- Phase C is now genuinely complete: all six original roadmap items are real production screens (five under evolved scope/numbering, this one under its original name and position).
- No navigation wiring was needed — unlike History (Sprint 021), Clean is already one of the 4 bottom-nav tabs (`TopLevelDestination.CLEAN`), reachable since Sprint 003.
- Deletion capability is now the clearly-named next piece of work for this feature, if a future sprint picks it up — this ADR states plainly what's still missing rather than leaving it implicit.
