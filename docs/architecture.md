# Architecture Overview

This document expands on the README's module map. It exists to satisfy
Sprint 003 Task 15 ("Module documentation") and to give Sprint 004+ authors
enough context to add features without re-deriving these decisions.

## Layering

```
┌─────────────────────────────────────────────────────────────┐
│  app (composition root)                                       │
│  ┌───────────────────────────────────────────────────────┐    │
│  │  feature:home  feature:security  feature:clean  ...    │    │
│  │  (Compose UI + ViewModel + StateFlow<UiState>)          │    │
│  └───────────────────────────────────────────────────────┘    │
│                          │ depends on                          │
│  ┌───────────────────────────────────────────────────────┐    │
│  │  domain (pure Kotlin UseCases)                          │    │
│  └───────────────────────────────────────────────────────┘    │
│                          │ depends on                          │
│  ┌───────────────────────────────────────────────────────┐    │
│  │  core:data / core:database / core:network / core:security│  │
│  │  (Repositories — the only layer that knows about Room,  │    │
│  │   Retrofit, DataStore, or EncryptedSharedPreferences)    │    │
│  └───────────────────────────────────────────────────────┘    │
│  ┌───────────────────────────────────────────────────────┐    │
│  │  core:common / core:model / core:designsystem / core:ui │    │
│  │  core:permissions / core:testing                          │  │
│  │  (shared primitives every layer above can use)            │  │
│  └───────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Why each module exists (Task 1)

| Module | Reason it's separate |
|---|---|
| `core:common` | `AppResult`/`AppError` and dispatcher qualifiers are used by literally every other module — putting them anywhere else would create a dependency cycle. |
| `core:model` | Shared data classes need one home so `core:data` and every `feature:*` agree on the same type without a feature depending on another feature. Empty through Sprint 003 by design; Sprint 004A added the Security domain models (`Threat`, `Detection`, `ScanSession`, `ScanResult`, `ScanStatistics`, `RiskLevel`, `ThreatType`, `ScanType`, `ScanSessionState`); Sprint 004B added the enumeration models (`ScanScope`, `ScanTarget`, `ScanRequest`, `FileMetadata`, `InstalledApplicationInfo`, `EnumerationFilter`). |
| `core:designsystem` | Owns Material 3 tokens (color/type/shape/spacing) — separated from `core:ui` (components) so a future rebrand touches one module, not every component. |
| `core:ui` | Reusable Compose components (Sprint 002.5 §9). Kept separate from `core:designsystem` because components change more often than tokens do, and feature authors should import "the button," not "the whole theme." |
| `core:data` | Repository implementations and DataStore-backed preferences — the seam between "business logic" (`domain`) and "how data is actually stored" (`core:database`, `core:network`). |
| `core:database` | Room. Isolated so swapping persistence technology later (unlikely, but possible) doesn't ripple into `core:data`'s Repository interfaces. |
| `core:network` | Retrofit/OkHttp/retry policy. Isolated for the same reason as `core:database`, and because the eventual OTA signature-update pipeline (Sprint 002 §8) will be the first real consumer of this module. |
| `core:security` | Encrypted storage and crypto/Play-Integrity abstractions — deliberately narrow-scoped (ADR 0008) so "sensitive value" stays a meaningful, reviewable category. |
| `core:permissions` | Sprint 001 Risk #4 (permission sprawl) is addressed architecturally here: `AppPermission` is a closed enum, not a place to pass arbitrary permission strings. |
| `core:enumeration` | Answers "what can be scanned" (Sprint 004B) — installed-app and filesystem discovery, entirely separate from `core:security`'s eventual detection logic. Implements `domain`'s `EnumerationRepository`. Deliberately split into an Android-free traversal algorithm (`FileTreeWalker`) and a thin Android-dependent layer (`ScanScopePathResolver`, `InstalledApplicationEnumerator`) so the traversal logic itself is unit-testable without Robolectric or a device. |
| `core:testing` | Shared test doubles/rules (`MainDispatcherRule`, `TestDispatchers`) so every module's test setup is identical, not reinvented per feature. |
| `domain` | UseCases that coordinate more than one Repository call — kept as pure Kotlin (ADR 0005) so business logic is unit-testable without any Android dependency. As of Sprint 004A: defines `SecurityRepository` (the contract, no implementation — that's Sprint 004B) and the UseCase layer around it (`StartScanSessionUseCase`, `CompleteScanSessionUseCase`, `CancelScanSessionUseCase`, `GetScanResultUseCase`, `GetLatestScanResultUseCase`, `ObserveScanHistoryUseCase`, `DeleteScanHistoryUseCase`). As of Sprint 004B: also defines `EnumerationRepository` (implemented in `core:enumeration`, same sprint) and its use cases (`EnumerateInstalledApplicationsUseCase`, `EnumerateFilesUseCase`, `ResolveScanTargetsUseCase`, `CreateScanRequestUseCase`). |
| `feature:*` (9 modules) | One per Sprint 002.5 screen area. Never depend on each other (ADR 0004) — this is the direct fix for Sprint 001's "engine and shell tightly interwoven" finding: a feature module can be rewritten or even deleted without touching another feature. |
| `benchmark` | Macrobenchmark module, isolated per AndroidX convention (it needs `com.android.test`, a different plugin from every other module). |
| `build-logic` | Composite build holding the `spaceav.android.*` convention plugins — keeps every module's own `build.gradle.kts` to ~10 lines instead of repeating compileSdk/Compose/Hilt setup 20 times. |

## Data flow — Security domain (implemented in Sprint 004A)

The domain layer (models, `SecurityRepository` contract, UseCases) is real
as of Sprint 004A. `core:data`'s implementation of `SecurityRepository`
(wiring it to Room) and the actual file/APK scanning logic that would call
`CompleteScanSessionUseCase` with real findings are Sprint 004B+ — nothing
below the UseCase layer exists yet, by this sprint's explicit scope.

```
(Sprint 004B+: some future ScanViewModel)
   │ calls
   ▼
StartScanSessionUseCase(ScanType.QUICK)      — creates + starts a session
   │
   ▼
(Sprint 004B+: actual file/APK analysis happens here — out of scope for 004A)
   │
   ▼
CompleteScanSessionUseCase(sessionId, statistics, threats)
   │ calls
   ▼
SecurityRepository.completeScanSession(...)   — contract only; Room-backed
                                                 implementation is Sprint 004B
   │
   ▼
AppResult<ScanResult>                          — returned to the caller,
                                                  ScanResult.isClean tells
                                                  a future UI whether to
                                                  show the "no threats
                                                  found" reassuring state
                                                  (Sprint 002.5 §15) or the
                                                  findings list
```

`ObserveScanHistoryUseCase` exposes `SecurityRepository.observeScanHistory()`
as a `Flow<List<ScanResult>>` directly rather than through the `UseCase`
base class, since a live-updating list isn't a one-shot `AppResult` — see
the KDoc on that class for the reasoning.

### Error handling in this layer
Every `SecurityRepository` method that can fail returns `AppResult<T>`
using the existing `AppError` sealed interface (ADR 0007), extended in
this sprint with `ScanSessionNotFound` and `InvalidScanConfiguration`
(ADR 0013) rather than a second, parallel error hierarchy.

## Data flow — Enumeration (implemented in Sprint 004B)

Answers "what can be scanned", never "is it dangerous" — that distinction
is architectural, not just a naming convention: `ScanTarget`, `FileMetadata`,
and `InstalledApplicationInfo` have no risk/severity field anywhere, on
purpose (see each model's KDoc).

```
CreateScanRequestUseCase(scanType, scopes)
   │ (pure construction, no repository call)
   ▼
ScanRequest
   │
   ▼
ResolveScanTargetsUseCase(request)
   │ calls
   ▼
EnumerationRepository.resolveScanTargets(request)
   │ implemented by EnumerationRepositoryImpl (core:enumeration), which for
   │ each ScanScope in the request either:
   │
   ├─▶ InstalledApplicationEnumerator.enumerate()       (PackageManager)
   │      → List<InstalledApplicationInfo>
   │
   └─▶ ScanScopePathResolver.resolve(scope)              (Context/Environment)
          → File root
          │
          ▼
       FileTreeWalker.walk(root, filter)                 (Android-free —
          → List<FileMetadata>                             java.io.File only)
   │
   ▼
AppResult<List<ScanTarget>>   — ready for a future scanning sprint to
                                 iterate over; nothing here has looked at
                                 file contents, hashes, or permissions yet
```

`FileTreeWalker` is deliberately isolated from every Android-specific type
so it's unit-testable with plain JUnit against real temp directories — no
Robolectric, no emulator. `ScanScopePathResolver` and
`InstalledApplicationEnumerator` are the only two classes in this module
that touch `Context`/`PackageManager` directly, keeping the
Android-dependent surface as small as possible.

## Threat Analysis Foundation (Sprint 004C — contracts and value objects only, no engine)

The plug-in seam every future detection engine implements against,
without `domain` ever needing to change:

```
core:model (pure Kotlin — the shared vocabulary)
   AnalyzerId          — value object, identifies which analyzer produced
                          a Detection (provenance — see ADR 0015)
   AnalyzerCapability   — FILE_ANALYSIS | APPLICATION_ANALYSIS
   AnalysisOutcome      — sealed: Clean | Flagged | Inconclusive
   Detection            — now carries analyzerId (breaking change from
                           Sprint 004A's shape, documented in ADR 0015)

domain/analyzer (pure Kotlin — the plug-in contracts)
   ThreatAnalyzer         — interface: id, capabilities, suspend fun
                            analyze(target: ScanTarget): AppResult<AnalysisOutcome>
                            ← ANY future engine (signature/heuristic/AI/
                              cloud-reputation/behavioral) implements this,
                              in its own module, added in a later sprint
   ThreatAnalyzerRegistry — interface: allAnalyzers(), analyzersFor(target)
                            ← no implementation yet; a later sprint likely
                              uses Hilt @IntoSet multibindings
   ScanTargetCapability.kt — pure mapping: ScanTarget -> AnalyzerCapability

domain/scoring (pure Kotlin — severity summarization, not detection)
   RiskScorer                — interface: score(detections) -> RiskLevel
   HighestSeverityRiskScorer — the one concrete implementation this sprint
                               ships: max severity among the Detections
                               given. Deliberately not detection logic —
                               it never decides IF something is a threat,
                               only summarizes already-found evidence.
```

**Orchestration (Sprint 004C Patch 2):**

```
AnalyzeScanTargetUseCase(target: ScanTarget)
   │ calls
   ▼
ThreatAnalyzerRegistry.analyzersFor(target)   — routes by AnalyzerCapability
   │
   ├─▶ analyzer 1.analyze(target) ─┐
   ├─▶ analyzer 2.analyze(target) ─┼─▶ AnalysisOutcomeAggregator.aggregate(outcomes)
   └─▶ analyzer N.analyze(target) ─┘        │
                                             ▼
                                   AppResult<AnalysisOutcome>
```

`AnalysisOutcomeAggregator`'s combining rule, in precedence order: any
`Flagged` outcome wins (with every Flagged outcome's Detections
concatenated, never dropped); failing that, any `Inconclusive` wins over
`Clean` (the app can't honestly claim "no threats found" if part of the
analysis couldn't reach a conclusion); only if every analyzer says
`Clean` does the aggregate say `Clean`. Zero registered analyzers for a
target's capability is reported as `Inconclusive` with an honest reason,
never silently treated as `Clean` — "nothing is looking at this" and
"this was checked and found clean" must never be indistinguishable to a
future caller.

This is the concrete proof that the plug-in architecture works:
`AnalyzeScanTargetUseCase` never references a specific analyzer
implementation, only the `ThreatAnalyzer`/`ThreatAnalyzerRegistry`
contracts from Patch 1.

**Still explicitly deferred:** converting an `AnalysisOutcome.Flagged`
into a persistable `Threat` (Sprint 004A's model) needs a `title`/
`description` for the user-facing record — real content, not placeholder
text. Generating that inside `domain` without going through Sprint
002.75's approved Vocabulary Dictionary and review process would be
exactly the kind of ad-hoc, unreviewed copy that content governance
(Sprint 002.75 §20) exists to prevent. That mapping is real, separate
work for a later patch/sprint, once it's clear whether that copy is
generated (and by what rule) or supplied by each analyzer itself.

**Patch 3 resolves this without inventing copy:**

```
AnalysisOutcome.Flagged
   │
   ▼
BuildThreatUseCase(outcome)
   │  riskLevel    ← RiskScorer.score(detections)          (Patch 1's plug-in point)
   │  threatType   ← detections.maxBy { it.riskLevel }.threatType
   │  title/desc   ← ThreatDescriptionProvider (Patch 3 — CONTRACT ONLY,
   │                  no implementation; see ADR 0016 for why)
   ▼
Threat   — ready for CompleteScanSessionUseCase (Sprint 004A) to persist
```

### Sprint 005: the full pipeline is now real, end to end

`RunScanRequestUseCase` composes every UseCase from 004A/004B/004C into
one working flow:

```
RunScanRequestUseCase(ScanRequest)
   │
   ├─▶ StartScanSessionUseCase(scanType)                          (004A)
   │
   ├─▶ ResolveScanTargetsUseCase(request) → List<ScanTarget>       (004B)
   │
   ├─▶ for each target: AnalyzeScanTargetUseCase(target)           (004C)
   │        → Flagged  → BuildThreatUseCase(outcome) → Threat      (004C)
   │        → Inconclusive → counted honestly (ADR 0017)
   │        → Clean     → no action
   │
   └─▶ CompleteScanSessionUseCase(sessionId, statistics, threats)  (004A)
          → AppResult<ScanResult>
```

Fail-fast on the first `AppResult.Failure` from any step. This is the
concrete, tested proof that three sprints' worth of independently-built
contracts actually compose — not just an architectural claim.

### Progress observability (Sprint 005 Feature Block 2)

`RunScanRequestUseCase` now publishes a `ScanProgress` snapshot after
starting the session, after enumeration resolves (`totalItems` becomes
known), and after every target is analyzed — via
`SecurityRepository.updateScanProgress()`, observable live through
`ObserveScanProgressUseCase` / `SecurityRepository.observeScanProgress()`.

This is the one place in the whole orchestration that deliberately breaks
the fail-fast pattern: a failed progress-snapshot write does NOT abort
the scan (ADR 0018). Every other `AppResult.Failure` in this UseCase
still aborts immediately — this is a narrow, explicit exception, not a
general softening of the fail-fast rule.

### Detection Engine Infrastructure (Sprint 006)

Three real behavior changes, all documented in ADR 0019:

**Concurrent analyzer execution.** `AnalyzeScanTargetUseCase` now runs
every applicable analyzer for a target concurrently (`async`/`awaitAll`)
via `AnalyzerExecutor`, instead of sequentially.

**Fault isolation — a breaking change from Sprint 004C's original
semantics.** Previously, any single analyzer's `AppResult.Failure`
aborted analysis for the whole target. Now, one broken/crashing analyzer
(caught by `AnalyzerExecutor`, including genuine thrown exceptions, not
just well-behaved `Failure` results) doesn't prevent other, working
analyzers from still contributing. Only if every applicable analyzer
fails does the method surface a `Failure` — visibly distinct from both
`Clean` and `Inconclusive`, since a total-analyzer-failure is a real
operational problem.

**Cooperative cancellation.** `RunScanRequestUseCase` checks
`coroutineContext.ensureActive()` between targets; a caller cancels a
running scan through ordinary structured concurrency (cancelling the Job
it's running in), not a bespoke API. On cancellation, the session is
transitioned to `CANCELLED` via a `NonCancellable`-wrapped cleanup write
before the `CancellationException` is rethrown — without that wrapper,
the cleanup write would itself be cancelled before running, leaving the
session stuck in `RUNNING` forever.

```
DefaultThreatAnalyzerRegistry(Set<ThreatAnalyzer>)   — real registration,
                                                        Hilt multibinding
                                                        wiring deferred
                                                        (ADR 0019 §1)
   │
   ▼
AnalyzeScanTargetUseCase(target)
   │ registry.analyzersFor(target) → applicable analyzers
   │
   ├─▶ AnalyzerExecutor.execute(analyzer1, target) ─┐  (concurrent,
   ├─▶ AnalyzerExecutor.execute(analyzer2, target) ─┼─  fault-isolated —
   └─▶ AnalyzerExecutor.execute(analyzerN, target) ─┘   each wrapped)
         │
         ▼
   AnalyzerExecutionOutcome(result, AnalyzerExecutionMetrics)
         │
         ▼
   successes aggregated via AnalysisOutcomeAggregator (004C);
   only if ALL fail does the use case surface a Failure
```


**Current real-world behavior, stated plainly:** with no `ThreatAnalyzer`
bound anywhere yet, every target today resolves to `Inconclusive`, so
`RunScanRequestUseCase` currently produces `ScanResult`s where
`itemsInconclusive == itemsScanned` and `isClean == false` — an honest
"nothing is actually checking yet" result. That's correct behavior for
where the project actually is, not a bug to paper over.

### Concurrent Scan Guarding (Sprint 007)

`RunScanRequestUseCase` now checks
`SecurityRepository.getActiveScanSession()` as its very first step. If a
scan is already `PENDING` or `RUNNING`, the call is rejected immediately
with `AppError.ScanAlreadyInProgress` — before `startScanSession` is ever
called, so a rejected second call creates no new session and leaves
nothing to clean up. This is a rejection policy, not a queueing one; see
ADR 0020 for why queueing wasn't implemented speculatively.

### Trusted Item Management (Sprint 008)

Real domain backing for the "Trusted List" screen named in Sprint 002.5's
UX spec: `TrustedItemRepository` (contract only — `AddTrustedItemUseCase`,
`RemoveTrustedItemUseCase`, `IsTrustedUseCase`, `ObserveTrustedItemsUseCase`),
backing `TrustedItem`/`TrustedItemType` models in `core:model`.

`addTrustedItem` is idempotent by `(identifier, type)` — re-adding an
already-trusted item returns the existing entry rather than creating a
duplicate. **Not yet wired into the scan pipeline** — `IsTrustedUseCase`
exists for a future sprint to call, but `RunScanRequestUseCase` doesn't
skip trusted targets yet. See ADR 0021 for why that integration (and the
`ScanStatistics` question it raises) is deliberately its own future step,
not folded in here.

## Navigation

Four bottom-nav destinations (`TopLevelDestination` enum) plus five
reachable-but-not-top-level destinations (Onboarding, Premium, History,
Notifications, RealTime), matching Sprint 002.5 §5's information
architecture exactly. Route constants are plain strings for Sprint 003
(ADR 0009) with a documented path to type-safe routes in Sprint 004.

## Theming

`core:designsystem`'s `SpaceAntivirusTheme` implements dynamic color
(Android 12+) with a fixed brand-seed fallback, per Sprint 002.5 §8. Dark
theme uses Compose's `isSystemInDarkTheme()` by default — no manual
light/dark toggle exists yet (not specified in any prior sprint as a
required feature; Settings could add one in Sprint 004 if desired).
