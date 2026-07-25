# ADR 0033: First scan execution UI — and a real gap between orchestration and progress observation

**Status:** Accepted

## Context
Sprint 020 enables the Scan button and wires it to the real production pipeline. Doing this surfaced a genuine architectural gap, found before writing any bridging code, not discovered by trial and error: `RunScanRequestUseCase` (Sprint 007+) is a single suspend call that returns only once the entire scan finishes — it never exposes the session id it creates internally until then. `ObserveScanProgressUseCase(sessionId)`, meanwhile, needs that id to start observing anything. There is no existing, single reactive path from "start a scan" to "watch its live progress."

This is exactly the kind of thing this sprint's brief said to stop and report rather than silently work around. It was reported directly, then bridged, rather than either blocked on or hidden — the reasoning for that choice follows.

## Decision: a bounded polling bridge, not a `RunScanRequestUseCase` redesign
`ScanViewModel.awaitActiveSession()` polls `GetActiveScanSessionUseCase` (already existing, one-shot) every 50ms, up to 20 attempts (1 second total), immediately after triggering the scan. Session creation is a single Room insert very early in `RunScanRequestUseCase`'s execution (before target resolution, before any actual analysis), so this window is generous relative to how long it actually takes in practice. If the poll times out — the scan failing before ever creating a session, or being rejected by the concurrent-scan guard (ADR 0020) — progress observation is simply skipped; the scan's own result still reaches the UI normally through the same code path.

**Why not redesign `RunScanRequestUseCase` instead** — the architecturally "cleaner" fix would be having it expose the session id immediately (e.g., returning a `Flow` or taking a callback). That's a real, invasive change to heavily-tested orchestration code spanning Sprints 005 through 016, touching many existing call sites and tests, entirely out of proportion for what a single UI sprint should carry. The polling bridge costs nothing in the domain layer and can be replaced later if a cleaner API is ever justified by a second consumer needing the same thing.

## Other decisions

### A separate `ScanViewModel`, not folded into `HomeViewModel`
Passive protection-status observation (`HomeViewModel`) and active scan orchestration (`ScanViewModel`) are different concerns of the same screen — each independently testable, each with its own state machine. `HomeRoute` composes both via two separate `hiltViewModel()` calls.

### No dialog for progress or results
`AppConfirmDialog` — the only dialog component in this design system — is explicitly documented as "reserved for genuinely blocking confirmations only... never used for informational content" (Sprint 002.5 §8/§9). Progress and completion are informational, not confirmations. Both render inline on Home instead: an `AppLinearProgress`/label pair while running, a dismissible `AppCard` banner on completion or error.

### No duplicate results detail
`ScanUiState.Completed` carries only a brief summary (clean/not-clean, threat count, items scanned) — not per-threat detail. Security Center (Sprint 019) already reactively shows full per-threat detail from the exact same `ObserveScanHistoryUseCase` this scan persists into; once the scan completes, Security Center reflects it automatically with zero additional wiring. Duplicating that UI here would be redundant, not additional value.

### Determinate progress, per the design system's own stated preference
`AppProgressIndicator.kt`'s own KDoc says progress should be determinate "wherever the underlying process reports one (scan, clean)." `ScanProgress.itemsProcessed / totalItems` is computed as a real fraction whenever `totalItems > 0`; indeterminate only for the brief pre-enumeration window `ScanProgress.starting()` represents.

### Double-trigger guard, verified carefully — including a real test-infrastructure gotcha
`startScan()` checks `_uiState.value is ScanUiState.Running` synchronously before launching anything, relying on `viewModelScope`'s real dispatcher (`Dispatchers.Main.immediate`) executing a freshly-launched coroutine's first statement synchronously when called from the main thread — which is how production actually behaves. Writing the test for this surfaced a real gap: `MainDispatcherRule` uses `StandardTestDispatcher`, which does *not* execute `launch{}` immediately — it merely schedules it. Three back-to-back `startScan()` calls with no intervening `runCurrent()` would all see the guard's state as unchanged and pass, since none of their launched coroutines would have run yet — not a production bug, but a test that would have silently verified nothing. Fixed by calling `runCurrent()` between the first and subsequent calls, so the guard is tested under conditions that actually exercise it.

## Consequences
- `RunScanRequestUseCase` are, for the first time, driven from a real user action, not just tests and instrumented smoke checks.
- The polling bridge is a documented, bounded piece of technical debt — worth revisiting if a second consumer ever needs the same "observe a scan I just started" capability, at which point extending `RunScanRequestUseCase`'s own API would be justified by more than one caller's convenience.
- Any future ViewModel test exercising a synchronous guard-check-before-launch pattern should check for this same `StandardTestDispatcher`-vs-`Dispatchers.Main.immediate` discrepancy explicitly, not assume `launch{}` runs immediately in tests the way it does in production.
