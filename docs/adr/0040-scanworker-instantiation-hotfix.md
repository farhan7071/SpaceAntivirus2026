# ADR 0040: Sprint 026.1 hotfix — ScanWorker instantiation on real devices

**Status:** Accepted

## Context
Real-device testing of Sprint 026 (Samsung Galaxy S9+, Android 9 / API 28; Xiaomi 23053RN02A, Android 15 / API 35) surfaced two issues. This ADR documents the full execution-flow trace performed before writing any fix, per this hotfix's own instruction to identify the root cause rather than apply a workaround.

## Issue 1: `NoSuchMethodException` constructing `ScanWorker`

### Traced execution flow
Settings toggle (`SettingsViewModel.onBackgroundProtectionToggled`) → `ScheduleBackgroundScanUseCase` → `BackgroundScanScheduler.schedulePeriodicScan` → `WorkManagerBackgroundScanScheduler.schedulePeriodicScan` (`core:workmanager`) → `WorkManager.getInstance(context).enqueueUniquePeriodicWork(...)` → **at the point the scheduled work actually fires** → WorkManager needs to construct a `ScanWorker` instance → its configured `WorkerFactory` is asked to do so.

Every step up to and including `enqueueUniquePeriodicWork` was already correct and unchanged by this hotfix — the crash happens later, entirely inside WorkManager's own worker-construction step, not in any of this project's own scheduling code.

### Root cause
WorkManager's default initializer (`androidx.startup`'s `InitializationProvider`, a `ContentProvider`) runs during `ContentProvider.onCreate()` — which the Android platform always executes **before** `Application.onCreate()`, on every API level. This is not new or version-specific behavior; it has been constant since `ContentProvider`s existed. `SpaceAntivirusApp` (Sprint 024) implemented `Configuration.Provider` expecting WorkManager's auto-initializer to read `workManagerConfiguration` (which references `@Inject lateinit var workerFactory: HiltWorkerFactory`) — but Hilt's field injection for a `@HiltAndroidApp` class happens via the generated base class's own `onCreate()`/`attachBaseContext()` handling, which — like all `Application` lifecycle code — necessarily runs *after* `ContentProvider.onCreate()`. Reading `workerFactory` that early is not guaranteed to see a correctly-injected instance, so WorkManager silently fell back to its own default, non-Hilt-aware `WorkerFactory`. That factory only knows how to construct a `Worker` via the plain two-argument `(Context, WorkerParameters)` constructor — not `ScanWorker`'s real `@AssistedInject` constructor, which also needs `RunScanRequestUseCase`. Reflection against the wrong constructor signature produces exactly the reported `NoSuchMethodException`.

**Why this affects Android 9 through 15 equally:** `ContentProvider`-before-`Application` ordering is a platform fundamental, not something that changed across the API range this app targets. The Samsung device was simply the first one this flow was actually exercised on; nothing about the trace above depends on API level.

### Fix
1. Disable WorkManager's default auto-initializer via a manifest `<meta-data>` override (`tools:node="remove"` on `androidx.work.WorkManagerInitializer`) — the standard, documented mechanism for this.
2. Call `WorkManager.initialize(this, workManagerConfiguration)` manually, synchronously, in `SpaceAntivirusApp.onCreate()`. Hilt's generated base class guarantees field injection completes before this override's body runs, for every API level — this is what makes the fix correct rather than incidentally-working.

This is the documented, standard resolution for this exact Hilt+WorkManager integration pattern (the same one Google's own Hilt+WorkManager guidance describes), not a workaround specific to this bug.

### A risk considered and deliberately not taken
An early draft of this fix dispatched the `WorkManager.initialize()` call to a background coroutine, reasoning that it might also help Issue 2's startup jank. This was reverted after tracing the consequence: `RECEIVE_BOOT_COMPLETED` (Sprint 025) means `SpaceAntivirusApp` can be launched as part of a boot-triggered start, and WorkManager's own bundled boot-rescheduling `BroadcastReceiver` needs a genuinely initialized `WorkManager` instance soon after boot. Deferring initialization asynchronously would risk that receiver firing before the background dispatch completes — undoing the entire point of Sprint 025's boot-persistence fix for an unverified performance gain. A real, if narrow, correctness regression is not an acceptable trade for a performance improvement that couldn't be confirmed. `WorkManager.initialize()` itself is also not the kind of call known to cost anywhere near two seconds in normal operation, further weakening the case for taking that risk.

## Issue 2: ~2 second startup frame skip (Android 15)

`MainActivity` (just `enableEdgeToEdge()` + `setContent{}`), `SpaceAntivirusApp.onCreate()`, `DataModule`'s Room/DataStore providers, and `OnboardingViewModel` (the app's actual start destination — trivial page-index state, no domain dependencies) were all audited directly for synchronous main-thread work reachable from cold launch. `Room.databaseBuilder(...).build()` and the `preferencesDataStore` delegate are both lazy by construction — neither touches disk until a real query/collection happens, confirmed by inspection of Room's and DataStore's documented behavior, not assumed.

No concrete, fixable contributor to the full ~2 second duration was found in this audit. Root-causing it precisely would require live on-device profiling (Perfetto/Android Studio Profiler) that this environment cannot perform. Rather than guess at a fix for an unconfirmed cause — which risks introducing exactly the kind of new bug this hotfix exists to avoid — this ADR records the audit performed and its result honestly. If profiling on a real device identifies a specific culprit, that's a well-scoped follow-up with a concrete target, not a redesign.

## Consequences
- `feature:settings/androidTest`'s `WorkManagerBackgroundScanSchedulerTest` already initializes its own isolated test WorkManager instance via `WorkManagerTestInitHelper.initializeTestWorkManager(...)`, independent of the app's production `Configuration.Provider`/manifest wiring — this is the standard, documented mechanism for testing WorkManager-integrated code and is unaffected by this hotfix. Worth confirming during real-device instrumented test verification, since it's the one place this fix's manifest change and the existing test infrastructure interact, even though the two are designed to be independent.
- `ScanWorkerTest` constructs `ScanWorker` directly via `TestListenableWorkerBuilder`'s own minimal `WorkerFactory`, never touching `SpaceAntivirusApp`'s `Configuration.Provider` or `WorkManager.initialize()` at all — unaffected by this hotfix.
- CHANGELOG.md is introduced by this hotfix (didn't exist before) — going forward, ADRs remain the detailed record; the changelog stays a concise, user-facing summary per release.
