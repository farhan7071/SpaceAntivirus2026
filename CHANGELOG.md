# Changelog

All notable changes to this project are documented here. Format loosely
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); this
file starts with Sprint 026's real-device hotfix rather than
retroactively documenting every prior sprint, since ADRs already serve
as this project's detailed historical record (`docs/adr/`).

## Sprint 026.1 — Hotfix

Real-device testing of Sprint 026 (Samsung Galaxy S9+ / Android 9 API 28,
Xiaomi 23053RN02A / Android 15 API 35) surfaced two issues before Sprint
026 could be considered production-ready.

### Fixed

- **Background Protection failed to schedule on real devices** —
  enabling the switch on Settings caused WorkManager to throw
  `NoSuchMethodException: ScanWorker.<init>(Context, WorkerParameters)`
  when attempting to construct `ScanWorker`. Root cause: WorkManager's
  default `ContentProvider`-based auto-initializer runs before
  `Application.onCreate()` on every Android version, so it could read
  `SpaceAntivirusApp`'s `Configuration.Provider` implementation before
  Hilt had field-injected `HiltWorkerFactory`, silently falling back to
  WorkManager's own non-Hilt-aware default factory. Not an Android-9-
  specific bug — the ordering issue is constant across API levels, it
  was simply the first device this was exercised on. Fixed by disabling
  WorkManager's default auto-initializer (manifest `<meta-data>`
  override) and calling `WorkManager.initialize(...)` manually from
  `Application.onCreate()`, which Hilt's generated base class always
  invokes after field injection completes. See ADR 0040.

### Investigated

- **~2 second startup frame skip** reported on Android 15. Audited
  `MainActivity`, `SpaceAntivirusApp`, `DataModule` (Room/DataStore
  providers), and `OnboardingViewModel` (the app's actual start
  destination) for synchronous main-thread work reachable from cold
  launch; found none — Room's `databaseBuilder().build()` and the
  DataStore delegate are both lazy by construction. Considered
  backgrounding the new `WorkManager.initialize()` call as a possible
  contributor, but reverted that approach: `RECEIVE_BOOT_COMPLETED`
  means this `Application` can launch as part of a boot-triggered start,
  and deferring initialization asynchronously would risk WorkManager's
  own bundled boot-rescheduling receiver firing before the deferred call
  completes — a real correctness regression traded for an unverified
  performance gain. Root-causing the full duration precisely needs live
  on-device profiling this environment cannot perform; no change was
  made here that couldn't be verified with real confidence.

## Sprint 026 — Production Settings screen

Exposes the background scan scheduling infrastructure built in Sprints
024–025 through a real Settings screen: a toggle to enable/disable
Background Protection and interval selection (Daily / Every 3 Days /
Weekly), persisted via a new `BackgroundProtectionPreferences` domain
contract backed by the existing DataStore-based preferences
infrastructure. See ADR 0039.
