# ADR 0020: Concurrent scan guarding

**Status:** Accepted

## Context
Sprint 006 gave `RunScanRequestUseCase` real cancellation support, but nothing in the pipeline ever checked whether a scan was *already* running before starting another. Two overlapping calls to `RunScanRequestUseCase` — plausible the moment a real UI exists (a user tapping "Scan now" twice, or a scheduled scan firing while a manual one is still in progress) — would create two independent `ScanSession`s, both writing progress and both eventually calling `completeScanSession`, with no coordination between them. `SecurityRepository` had no method to even ask "is anything active right now."

## Decision
1. Added `SecurityRepository.getActiveScanSession(): AppResult<ScanSession?>` — returns the session currently in `PENDING` or `RUNNING` state, if any. `Success(null)` is the normal "nothing running" case, not a failure.
2. Added `GetActiveScanSessionUseCase`, matching the existing `NoParamsUseCase` pattern — the check a future "Scan now" UI would make before offering to start another scan.
3. Added `AppError.ScanAlreadyInProgress(activeSessionId: String)` — additive to the existing closed `AppError` set (ADR 0007/0013), not a new parallel hierarchy.
4. `RunScanRequestUseCase.execute()` now checks `getActiveScanSession()` as its very first step, before `startScanSession` is ever called. If a scan is already active, it returns `AppResult.Failure(AppError.ScanAlreadyInProgress(...))` immediately — no new session is created, so there's nothing to clean up on this path.

## Consequences
- A future UI can disable/hide a "Scan now" action while `GetActiveScanSessionUseCase` reports a non-null session, or handle `ScanAlreadyInProgress` by surfacing "a scan is already running" rather than silently queuing or racing a second one.
- This is a **rejection** policy, not a **queueing** policy — a second scan request while one is active is refused outright, not deferred to run after the first completes. Queueing is a reasonable future enhancement if a real need for it shows up (e.g. a scheduled scan arriving mid-manual-scan), but wasn't implemented speculatively here — it would need a real queue/scheduling concept this project doesn't have yet, and rejection is the simpler, more honest default until a concrete case demands otherwise.
- `FakeSecurityRepository.getActiveScanSession()` scans its in-memory `sessions` map for the first `PENDING`/`RUNNING` entry — adequate for testing coordination logic, not a realistic concurrent-access simulation (the fake was never meant to be one; see its own class KDoc from Sprint 004A).
- No existing tests were affected — every test constructs a fresh `FakeSecurityRepository`, which reports no active session by default, so the new guard check is transparent to all of Sprint 005/006's existing test coverage.
