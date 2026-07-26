# ADR 0039: Production Settings screen — exposing background protection controls

**Status:** Accepted

## Context
Sprints 024–025 built real, working, fully-tested background scan infrastructure — `ScheduleBackgroundScanUseCase`, `CancelBackgroundScanUseCase`, a configurable interval — that nothing in the app had ever called outside their own tests, by deliberate design (ADR 0037/0038's "foundation, not activation"). Before writing any code this sprint, current `main` was verified directly: Sprint 025 is genuinely present, `feature:settings` is still Sprint 003's literal placeholder, and no duplicate scheduling implementation exists anywhere. This sprint is the deliberate next increment both ADRs already named: a real Settings screen that finally lets a user turn this infrastructure on.

## Decisions

### 1. Zero changes to `core:workmanager`
`ScheduleBackgroundScanUseCase`/`CancelBackgroundScanUseCase` are consumed exactly as they already existed — confirmed directly via `git status` before committing, not assumed. This is the clearest possible evidence that "reuse the existing production scan pipeline... no duplicate scheduling logic" was followed, not just claimed.

### 2. Persist-only-on-confirmed-success, throughout
`SettingsViewModel` never writes to `BackgroundProtectionPreferences` speculatively — every write happens only after the corresponding `ScheduleBackgroundScanUseCase`/`CancelBackgroundScanUseCase` call has already returned `AppResult.Success`. This is what makes `lastScheduledAtEpochMillis` an honest "last scheduled state" signal rather than an assumption: it can never silently drift from what WorkManager actually has scheduled. Chosen specifically to avoid needing a live WorkManager query (`getWorkInfosForUniqueWorkFlow` or similar) whose exact availability at this project's pinned WorkManager version couldn't be confidently verified — the invariant achieves the same honesty more simply and with less new API-surface risk.

### 3. `UserPreferencesDataSource` extended, not replaced — "no other storage mechanism," applied literally
Three new DataStore keys added to the existing low-level wrapper. The domain-facing implementation, `DataStoreBackgroundProtectionPreferences` (new, `core:data`), is a thin adapter delegating to it — kept separate from `UserPreferencesDataSource` itself so a future, differently-scoped domain contract (e.g. notification preferences) can also wrap the same low-level source without `UserPreferencesDataSource` needing to implement multiple, unrelated domain interfaces directly.

`recordEnabled`/`recordDisabled` write their fields via a single `DataStore.edit{}` transaction each — enabled, interval, and timestamp change together when turning protection on, avoiding any window where a reader could observe an inconsistent partial state.

### 4. New `BackgroundProtectionPreferences` domain contract, deliberately narrow
Not a general-purpose `SettingsRepository` — that would invite scope creep into preferences this sprint has no reason to touch (analytics, notifications), which already have their own working, if currently unused, access pattern. `core:data` implements a domain contract for the first time in this project's history, requiring it to depend on `:domain` for the first time too — the same dependency direction every prior repository-implementing module has always had.

### 5. Six new, granular UseCases, matching established precedent
Three observe (`Flow`-returning) and three write (schedule-tied `recordEnabled`, `recordDisabled`, and independent `setScanInterval` for changing the interval while disabled). Same single-purpose granularity as `ObserveScanHistoryUseCase`/`ObserveTrustedItemsUseCase` rather than one broad "settings" UseCase — `RecordBackgroundProtectionEnabledUseCase` takes a small `RecordBackgroundProtectionEnabledParams` data class (interval + timestamp together) since `UseCase<Params, Result>` takes exactly one params type.

### 6. New `AppError.InvalidScheduleConfiguration` reused as-is, not duplicated
Already added in Sprint 025 for exactly this purpose; this sprint is its first real caller.

## Consequences
- Two real, non-obvious testing bugs were found and fixed while writing `SettingsViewModelTest`, both worth naming precisely since they're the kind of thing that could recur in future ViewModels reading their own `uiState.value` synchronously inside an action handler:
  1. `uiState.value` does not reflect real data until something actively collects the `stateIn(WhileSubscribed(...))`-backed flow — merely reading `.value` doesn't subscribe. Every test now collects via `uiState.test{}`, awaiting the initial `Loaded` emission, before calling any action method that reads `.value` internally.
  2. Mocked `observe*` Flows are static — calling a separate mocked "write" UseCase never causes them to re-emit, so success-path tests use `runCurrent()` (to force the launched coroutine to actually execute) followed by `cancelAndIgnoreRemainingEvents()`, not a further `awaitItem()` that would hang waiting for an emission that structurally cannot arrive. Only genuinely value-changing paths (the transient error flow) correctly produce a new emission to await.
- Preference persistence is tested against a real, temp-file-backed `DataStore` (`PreferenceDataStoreFactory.create`), not a mock — the same "prefer real infrastructure" discipline this project has followed since Sprint 010's Room/Hilt-graph testing, extended here to DataStore for the first time.
- Real file deletion (ADR 0036) remains the one other clearly-named, intentionally deferred capability in this project.
