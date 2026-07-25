# ADR 0037: Background scan infrastructure — Phase D's first increment

**Status:** Accepted

## Context
Phase D's goal is production background protection. This sprint is deliberately scoped to the smallest real, working slice: the ability to schedule and run a background scan reusing the existing production pipeline — not notifications, not quarantine, not real-time monitoring, all explicitly excluded.

## Decisions

### 1. `ScanWorker` reuses `RunScanRequestUseCase` directly — no second scan implementation
The worker's `doWork()` builds a `ScanRequest` and calls the exact same UseCase `ScanViewModel` (Sprint 020) already calls for a manual "Scan Now" tap — same scope (`ScanScope.InstalledApplications`, the only scope any real `ThreatAnalyzer` can evaluate), same `ScanType.QUICK`. An automatic background scan checks the same thing a manual one does, not something different or lesser. This also means the existing concurrent-scan guard (ADR 0020) applies automatically: if a manual scan is already running when the worker fires, `RunScanRequestUseCase` rejects the background attempt the same way it would reject a second manual one — no new guarding logic was needed.

### 2. `CoroutineWorker`, not the older callback-based `Worker`
This is the entirety of what "lifecycle-safe execution" required building: `CoroutineWorker.doWork()` runs on a WorkManager-managed coroutine scope tied to the worker's own lifecycle, cancelled automatically if the system stops the worker. Nothing extra was added for this property — choosing `CoroutineWorker` is what "Android's recommended architecture" means in practice for this use case.

### 3. `AppResult` → `WorkManager.Result` mapping, reasoned per branch
- `Success` → `Result.success()`.
- `Failure(ScanAlreadyInProgress)` → `Result.success()` — the concurrent-scan guard doing its job isn't this worker failing; retrying would just hit the same guard again for no benefit.
- `Failure(PermissionMissing)` → `Result.failure()` — retrying can't grant a permission; `Result.failure()` (not `retry()`) is the honest signal that this attempt is done, not pending.
- Any other `Failure` → `Result.retry()` — the honest default for failures that plausibly resolve on their own, deferring to WorkManager's own backoff policy.

### 4. Contract in `domain`, real implementation in a new `core:workmanager` module
`BackgroundScanScheduler` (domain, pure Kotlin, no WorkManager type crosses it) follows the exact same shape every prior repository in this project has used since Sprint 004B. `WorkManagerBackgroundScanScheduler` is the only class in the entire project that touches WorkManager APIs directly. `ScanWorker` lives in the same module since it's small and directly coupled to the scheduler that enqueues it — same reasoning ADR 0027/0028 gave for keeping small analyzers in `core:analysisengine` rather than spinning up a module per class.

### 5. Fixed 24-hour interval, `setRequiresBatteryNotLow(true)` only, no network constraint
No product requirement specified a cadence. 24 hours is a deliberate, reasonable default — meaningful frequency, far above WorkManager's hard 15-minute platform minimum, not an aggressive battery/resource consumer for what's meant to be background protection, not real-time. No network constraint: `RunScanRequestUseCase`'s entire pipeline is on-device (confirmed by re-checking, not assumed — no network call exists anywhere in it, Sprints 004B–021), so requiring connectivity would be an incorrect restriction, not a sensible one.

### 6. Not wired to activate automatically — deliberately
`ScheduleBackgroundScanUseCase` exists, is fully real, and is fully tested, but nothing in this sprint calls it. No Settings toggle exists yet to let a user opt in or out of automatic background scanning, and silently enabling it for every install without that product/consent decision isn't something this sprint should make unilaterally. This is "the foundation" in the literal sense this sprint's own brief used the word — the next increment (a Settings toggle calling this UseCase) is a small, additive UI change against infrastructure that already exists and already works, not a redesign.

### 7. `Configuration.Provider` on `SpaceAntivirusApp` — the one real API-surface uncertainty in this sprint
WorkManager's default initializer (already active via the manifest's `androidx.startup` provider merge — no manifest change needed) automatically detects an `Application` implementing `Configuration.Provider` and uses it instead of the default configuration. This project's pinned WorkManager version (2.9.0) should expose `workManagerConfiguration` as a Kotlin property rather than the older `getWorkManagerConfiguration()` function form — this could not be verified against a real compiler in this sandbox. Flagged explicitly rather than guessed past silently: if Gradle Sync reports otherwise, this is a single-line, isolated compatibility fix, not an architectural one — nothing else in this sprint depends on which form is correct.

### 8. Testing: real `TestListenableWorkerBuilder` and real test-mode `WorkManager`, not mocks of either
`ScanWorkerTest` uses `TestListenableWorkerBuilder`'s `setWorkerFactory` hook to construct `ScanWorker` directly with a mocked `RunScanRequestUseCase` — no Hilt test infrastructure needed, since `@AssistedInject` constructors remain plain, directly-callable Kotlin constructors regardless of the annotation. `WorkManagerBackgroundScanSchedulerTest` uses `WorkManagerTestInitHelper` for a genuine test-mode `WorkManager` instance, inspecting real `WorkInfo` state — the same "prefer real infrastructure over fragile mocks" discipline this project has followed since Sprint 010's Room/Hilt-graph testing.

## Consequences
- This is genuinely new dependency surface for the project: `androidx.work:work-runtime-ktx`, `androidx.work:work-testing`, and `androidx.hilt:hilt-work`/`hilt-compiler` (the latter two reusing the exact version already proven stable via `hilt-navigation-compose:1.2.0`, since they ship from the same `androidx.hilt` release train). `work = "2.9.0"` is a well-established, stable release from the same era as this project's other pinned dependency versions.
- Real file deletion (Clean UI's own deferred gap, ADR 0036) and background scan activation (this ADR's #6) are now both clearly-named, intentionally deferred next increments — not silently missing capability, but capability whose activation was deliberately left to a future, explicit product decision.
