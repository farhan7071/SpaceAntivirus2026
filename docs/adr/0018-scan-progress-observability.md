# ADR 0018: ScanProgress observability, and why progress publishing is best-effort

**Status:** Accepted

## Context
`ScanProgress` was named in this project's original architecture planning
alongside `ScanSession`/`ScanStatistics`/`ScanSummary`, but was never
actually built across Sprints 004A/004B/004C — nothing needed to observe
mid-scan state before `RunScanRequestUseCase` (Feature Block 1) existed to
report it. With the orchestration pipeline now real, a scan enumerating
many targets runs as a single opaque suspend call with no intermediate
signal — a future UI has no way to show "scanning 47 of 200," and no way
to distinguish "still running" from "hung," beyond `ScanSession`'s
coarse PENDING/RUNNING/COMPLETED state.

A second question came up while wiring this: should a failure to persist
a progress snapshot abort the whole scan? The scan itself might have
already done real, correct analysis work by that point.

## Decision
1. Added `ScanProgress` (core:model): `sessionId`, `itemsProcessed`,
   `totalItems`, `threatsFoundSoFar`, plus `isComplete` and a
   `starting(sessionId)` factory for the brief pre-enumeration state where
   `totalItems` isn't known yet (a valid state, not an error — see the
   model's own KDoc).
2. Extended `SecurityRepository` with `observeScanProgress(sessionId): Flow<ScanProgress>`
   and `updateScanProgress(progress): AppResult<Unit>` — contract only,
   matching every other repository method's pattern (no implementation
   yet; `SecurityRepository` as a whole still has none, per Sprint 004A).
3. Added `ObserveScanProgressUseCase`, matching `ObserveScanHistoryUseCase`'s
   established shape (Flow exposed directly, not forced through the
   AppResult-wrapped UseCase base class).
4. `RunScanRequestUseCase` now publishes a `ScanProgress` snapshot after
   starting the session, after enumeration resolves, and after every
   single target is analyzed.
5. **Progress publishing is deliberately NOT fail-fast.** Every other
   step in `RunScanRequestUseCase` aborts the whole run on
   `AppResult.Failure` (this project's established pattern, ADR 0007).
   Progress publishing is the one deliberate exception: its `AppResult`
   is discarded rather than checked. Losing a progress snapshot means a
   future UI briefly shows a stale percentage — a real but minor UX
   gap. Aborting an entire scan and discarding already-completed,
   correct analysis work because a progress *snapshot write* failed
   would make the scan's core reliability depend on a feature whose
   entire purpose is secondary observability. This is a narrow,
   explicitly scoped exception to fail-fast, not a general softening of
   it — every other `AppResult.Failure` in this UseCase still aborts.

## Consequences
- A future progress UI can call `ObserveScanProgressUseCase(sessionId)`
  and get live updates throughout a running scan, including the
  transition to `isComplete == true` right before the final `ScanResult`
  is available via `ObserveScanHistoryUseCase`/`GetScanResultUseCase`.
- `FakeSecurityRepository` gained a `publishedProgress` list (every
  snapshot ever published, in order) specifically so tests can assert on
  the full progression, not just the latest value — and a separate
  `forcedProgressUpdateFailure` distinct from `forcedFailure`, so tests
  can exercise the best-effort behavior (progress fails, scan still
  succeeds) independently from the fail-fast core-pipeline behavior
  (already covered by existing tests).
- Whichever future sprint implements `SecurityRepository` for real should
  treat `updateScanProgress` as safe to implement with weaker durability
  guarantees than `completeScanSession` — e.g., an in-memory/DataStore
  cache is a reasonable implementation choice; it doesn't need the same
  transactional care as persisting a final `ScanResult`.
- If a future feature genuinely needs progress-write failures to be
  visible (e.g., surfacing "progress tracking unavailable" in a UI), that
  UI can independently observe `updateScanProgress`'s `AppResult` by
  wrapping `observeScanProgress`'s staleness itself — this ADR's decision
  is specifically about `RunScanRequestUseCase` not aborting because of
  it, not a claim that the failure is unobservable everywhere.
