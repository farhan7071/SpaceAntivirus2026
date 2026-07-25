# ADR 0038: Configurable background scan scheduling, and boot-lifecycle persistence

**Status:** Accepted

## Context
Sprint 024 built the background scan foundation but left two real gaps, both confirmed directly against the repository before writing any code: `ScheduleBackgroundScanUseCase`'s interval was a hardcoded implementation constant, not something a future Settings toggle could actually configure; and nothing declared `RECEIVE_BOOT_COMPLETED`, meaning a scheduled periodic scan would silently stop firing after any device reboot until the user happened to relaunch the app — a real "background scheduler lifecycle management" gap, not a hypothetical one.

## Decisions

### 1. `schedulePeriodicScan` takes `intervalHours` as an explicit parameter, not a hardcoded constant
`BackgroundScanScheduler.DEFAULT_INTERVAL_HOURS` (still 24, unchanged from Sprint 024) now lives on the domain interface's companion object, not the implementation — "what the default is" is a contract-level fact a future caller needs, not an implementation detail. `MIN_INTERVAL_HOURS = 1` is validated explicitly in `WorkManagerBackgroundScanScheduler`, returning the new `AppError.InvalidScheduleConfiguration` rather than trusting WorkManager's own internal clamping — a caller should get an honest, explicit rejection, not a silently-altered interval.

`AppError.InvalidScheduleConfiguration` is a new, distinct case rather than reusing `InvalidScanConfiguration` — that case's own name is specifically about a scan request, and stretching it to also mean "an invalid scheduling interval" would misdescribe the error to anything reading the category later. Confirmed safe to add: every existing exhaustive `when` over `AppError` in the codebase already has an `else` branch, checked directly rather than assumed.

`ScheduleBackgroundScanUseCase` changed from `NoParamsUseCase<Unit>` to `UseCase<Long, Unit>` to carry this parameter through — Kotlin doesn't allow an overriding method to introduce a default value the abstract base didn't declare, so callers (a future Settings ViewModel) pass an explicit value, defaulting to `BackgroundScanScheduler.DEFAULT_INTERVAL_HOURS` themselves if they want the standing default.

**Still deliberately not wired to activate.** This is "user-configurable scheduling infrastructure" in the same literal sense ADR 0037 used the word "foundation" — the capability to request a specific interval is real and fully tested; nothing yet lets a user actually choose one, since no Settings UI exists. The next increment remains a small, additive UI change against infrastructure that already works.

### 2. `RECEIVE_BOOT_COMPLETED` declared, no custom `BroadcastReceiver` written
WorkManager bundles its own boot-rescheduling component in its AAR, merged into the app manifest automatically; it re-registers already-persisted periodic work with the OS scheduler after reboot, but only functions if the consuming app declares this normal-protection-level permission. Declaring it is the complete fix — writing a custom receiver that also called `ScheduleBackgroundScanUseCase` again would be exactly the "duplicate scheduling logic" this sprint's own instructions prohibit, re-implementing something WorkManager already does internally.

### 3. `setRequiresStorageNotLow(true)` and explicit `setBackoffCriteria`
Both are small, well-justified constraint refinements, not new architecture. Storage-not-low is reasonable general hygiene for any background work in an app that also does file enumeration (`EnumerationRepository`), not specific to scan content. Explicit `EXPONENTIAL` backoff starting at `WorkRequest.MIN_BACKOFF_MILLIS` makes previously-implicit WorkManager default behavior visible and intentional, appropriate for `ScanWorker`'s `Result.retry()` failures, which ADR 0037 already judged as plausibly self-resolving.

## Consequences
- Every existing test calling `schedulePeriodicScan()`/`ScheduleBackgroundScanUseCase()` with no arguments needed updating to pass an explicit interval — found and fixed directly (`WorkManagerBackgroundScanSchedulerTest`, `ScheduleBackgroundScanUseCaseTest`), not left to surface as a build failure.
- Real file deletion (ADR 0036) and background scan activation (ADR 0037, still true after this sprint) remain the two clearly-named, intentionally deferred next increments for their respective features.
