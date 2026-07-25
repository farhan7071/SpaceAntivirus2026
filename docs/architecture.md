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
duplicate. Wired into the scan pipeline in Sprint 009 (below).

### Trusted Item Scan-Pipeline Wiring (Sprint 009)

`RunScanRequestUseCase` now checks `IsTrustedUseCase` before analyzing
each target. A trusted target is skipped entirely — no analyzer ever runs
against it — and counted in `ScanStatistics.itemsTrusted`, a third
counter orthogonal to `itemsScanned`/`threatsFound`/`itemsInconclusive`
(ADR 0022, same breaking-change category as ADR 0015/0017).

Two deliberate asymmetries worth knowing:
- **`ScanResult.isClean` ignores `itemsTrusted`** but NOT
  `itemsInconclusive` — trusting an item is user consent, not a coverage
  gap, so it shouldn't make an otherwise-clean scan look less clean.
- **Trust-check failures fail SAFE, not just fail-open.** Unlike progress
  publishing (ADR 0018, where losing an update is harmless), a failed
  trust check defaults to `false` (not trusted → scan it anyway) rather
  than `true` — the two possible defaults aren't equally safe for a
  security product, so the failure is tolerated by picking the safer
  outcome, not by shrugging at it.

### SecurityRepository's Real Persistence Schema — entities/DAOs only (Sprint 010)

`SecurityRepository` has been contract-only since Sprint 004A. This
sprint gives it a real Room schema in `core:database` — but deliberately
**only** the schema (entities + DAOs), not the repository implementation
itself. That split was a direct response to a real constraint: Room's
KSP-generated code and any instrumented test cannot be compiled or run in
this project's authoring sandbox, unlike every prior sprint's pure-Kotlin
`domain` work. Building the smallest, most inspectable unit of real Room
code first — and deferring the actual entity↔domain-model mapping layer
to its own, separately-reviewed Sprint 011 — was the explicit tradeoff
made to manage that risk. See ADR 0023 for the full reasoning, including
why `ScanProgress` is deliberately NOT persisted (it stays in-memory
even in the eventual real implementation) and why the schema bump uses
`fallbackToDestructiveMigration()` rather than a real `Migration` (no
real user data has ever existed in this schema to preserve).

```
ScanSessionEntity  (1)───(0..1) ScanStatisticsEntity   [CASCADE delete]
       │
       └──(0..n) ThreatEntity ──(0..n) DetectionEntity  [CASCADE delete,
                                                           both hops]
```

DAOs return/accept entities directly — no `@Relation`/`@Transaction`
multi-table joins. Assembling a full `ScanResult` from these tables is
Sprint 011's job, done in Kotlin code over several simple DAO calls
rather than one complex relational query.

### SecurityRepositoryImpl — closing ADR 0014's long-deferred item (Sprint 011)

`core:securitydata` (new module — not `core:security`, which is Sprint
003's encryption/Keystore module) provides `SecurityRepositoryImpl`, the
first production implementation `SecurityRepository` has ever had. Wired
via `@Binds`, following the exact pattern `core:enumeration` established
in Sprint 004B, per ADR 0014's own prediction three sprints ahead of it
happening.

```
domain's SecurityRepository (contract, since Sprint 004A)
   ▲
   │ @Binds
   │
SecurityRepositoryImpl (core:securitydata, Sprint 011)
   │
   ├─▶ SecurityEntityMappers.kt   — entity <-> domain, enums via .name/valueOf
   │
   ├─▶ ScanSessionDao / ScanStatisticsDao / ThreatDao / DetectionDao
   │      (core:database, Sprint 010 — provided via core:data's DataModule)
   │
   └─▶ ConcurrentHashMap<sessionId, MutableStateFlow<ScanProgress>>
          (in-memory only, per ADR 0023 — never touches Room)
```

Two things worth knowing if you're touching this code later:
- **`completeScanSession` is the only method wrapped in
  `AppDatabase.withTransaction`** — it's the only one writing to more than
  one table. Every other multi-row effect (history deletion) relies on
  SQLite's own CASCADE foreign keys instead, already atomic as a single
  statement.
- **`observeScanHistory()`'s correctness depends on an invariant**: it
  only re-emits because `completeScanSession` always writes
  `scan_sessions` in the same transaction as any statistics/threats
  change. Writing those independently in a future change would make this
  Flow silently go stale. See ADR 0024 and the method's own KDoc.

Tests are instrumented (`androidTest`), not JVM unit tests — same
reasoning as Sprint 010: no Robolectric, and reliably mocking
`AppDatabase.withTransaction` (a Kotlin extension function) would be
fragile in a way that looks like coverage without actually verifying
transaction behavior. A real in-memory database is used instead.

### TrustedItemRepositoryImpl — the same treatment, in one sprint instead of two (Sprint 012)

`TrustedItemRepository` has been contract-only since Sprint 008 — the
same situation `SecurityRepository` was in before Sprints 010/011.
`AppDatabase`'s own KDoc had explicitly flagged `TrustedItemEntity` as
"deliberately NOT here yet," a direct pointer to this sprint.

**Why one sprint this time, not a two-sprint split like Security got:**
`TrustedItem`'s schema is a single standalone table — no foreign keys, no
multi-row write needing transactional atomicity. The risk that justified
splitting Sprint 010/011 (subtle Room relational/transactional mistakes,
hard to catch without a compiler) doesn't apply at the same scale here.
See ADR 0025 for the full reasoning — the caution was proportionate to
actual complexity each time, not a fixed process.

```
domain's TrustedItemRepository (contract, since Sprint 008)
   ▲
   │ @Binds
TrustedItemRepositoryImpl (core:trusteddata, Sprint 012)
   │
   ├─▶ TrustedItemEntityMappers.kt
   └─▶ TrustedItemDao (core:database — no foreign keys, no transactions)
```

Unlike `core:securitydata`, this module doesn't depend on Room directly —
`TrustedItemRepositoryImpl` never calls a Room framework API itself, only
plain suspend functions on `TrustedItemDao`.

With this sprint, every repository this project has defined now has a
real implementation: `EnumerationRepository` (004B), `SecurityRepository`
(011), `TrustedItemRepository` (012). `IsTrustedUseCase` — wired into
`RunScanRequestUseCase` back in Sprint 009 against
`FakeTrustedItemRepository` — now runs against real, persisted data for
the first time, with no `domain` changes required to make that true.

### Closing the analysis-side DI gaps (Sprint 013)

A Sprint 012 status review found two real gaps: `ThreatAnalyzerRegistry`
and `RiskScorer` both had concrete implementations
(`DefaultThreatAnalyzerRegistry`, `HighestSeverityRiskScorer`) that were
never bound into the Hilt graph — `HighestSeverityRiskScorer` didn't even
have an `@Inject` constructor. Neither had ever caused a build failure
only because no feature ViewModel yet injects anything downstream of
them (every feature module is still the Sprint 003 placeholder).

New module `core:analysisengine` hosts `AnalysisEngineBindingModule`:

```
@Multibinds Set<ThreatAnalyzer>              — legitimizes the empty-set
                                                case; the first real
                                                analyzer (Phase A) adds an
                                                @IntoSet contribution in
                                                its own module, no change
                                                needed here
@Binds ThreatAnalyzerRegistry  -> DefaultThreatAnalyzerRegistry
@Binds RiskScorer              -> HighestSeverityRiskScorer
```

Verified by a real `@HiltAndroidTest` (`AnalysisEngineBindingModuleTest`,
`app/src/androidTest`) using a new `HiltTestRunner` — whether Dagger's
compile-time graph validation actually accepts these declarations isn't
something a JVM unit test can check at all; it needs Hilt's own
annotation processor running against the real, fully-assembled `:app`
component.

**Deliberately still open:** `ThreatDescriptionProvider` has no binding
and no implementation — closing the two gaps above does NOT make
`BuildThreatUseCase`/`RunScanRequestUseCase` fully Hilt-constructible yet.
That copy needs Sprint 002.75's content-governance review (ADR 0016),
explicitly scoped to a later phase, not absorbed into this sprint as a
stub. See ADR 0026.

### The first real ThreatAnalyzer (Sprint 014)

Before writing any analyzer code, a real gap was found: `InstalledApplicationInfo`
(Sprint 004B) carried no permission data at all, despite its own KDoc
saying permission analysis belonged to "a later sprint's analyzers." Now
this sprint. Extended (ADR 0027, a 4th breaking model change following
the same pattern as ADR 0015/0017/0022): `requestedPermissions: List<String>`,
populated by `InstalledApplicationEnumerator` requesting
`PackageManager.GET_PERMISSIONS`.

`SuspiciousPermissionPatternAnalyzer` (`core:analysisengine`) flags two
permission COMBINATIONS, never a single permission alone:

```
SMS interception:     (READ_SMS or RECEIVE_SMS) + INTERNET
Device-admin lock:     BIND_DEVICE_ADMIN + INTERNET
```

Both produce `RiskLevel.ATTENTION`, never `ACTION_NEEDED` — a heuristic
this coarse shouldn't claim more certainty than it has. System apps are
excluded entirely before either rule runs, not scored lower — a false
"malware" flag on a core Android component would be a severe,
trust-destroying false positive.

Registered via `@Binds @IntoSet` directly in Sprint 013's
`AnalysisEngineBindingModule` — small enough that it didn't need the
separate module ADR 0026 predicted future analyzers would live in.

```
RunScanRequestUseCase (real, since Sprint 007)
   │
   ▼
AnalyzeScanTargetUseCase → ThreatAnalyzerRegistry.analyzersFor(target)
   │                              (now returns 1 analyzer for
   │                               ApplicationTarget, 0 for FileTarget)
   ▼
AnalyzerExecutor.execute(SuspiciousPermissionPatternAnalyzer, target)
   │
   ▼
AnalysisOutcome.Flagged | Clean   — the first non-Inconclusive real
                                     results this pipeline has ever
                                     produced
```

`AnalysisPipelineIntegrationTest` (`core:analysisengine/src/test`) runs
the real registry, executor, aggregator, use case, and analyzer together
as a plain JVM test — no fakes, no Android needed, since none of these
five classes touch it. The first time in this project real multi-class
integration has been verifiable without either a domain-layer fake or a
full instrumented test.

### The second ThreatAnalyzer, and deduplication (Sprint 015)

`AppIdentityImpersonationAnalyzer` (`core:analysisengine`) evaluates a
genuinely different risk dimension than Sprint 014's analyzer — identity
(is this app pretending to be a well-known brand?) rather than
capability (permission combinations). Flags only an **exact** label
match against a short, high-confidence list of well-known app names
combined with a package-name mismatch; both conditions required, both
conservative by design. Uses `ThreatType.POTENTIALLY_UNWANTED_APPLICATION`,
distinct from Sprint 014's `SUSPICIOUS_PERMISSION_USAGE`.

`AnalysisOutcomeAggregator` (`domain`, unchanged since Sprint 004C until
now) gained real deduplication: concatenated `Detection`s are collapsed
by exact `(threatType, riskLevel, evidenceDescription)` match before
reaching the final outcome. This does not weaken the aggregator's
existing "never drop evidence" rule — that rule is about never
discarding a Detection because a *different* analyzer disagreed;
deduplication only ever collapses detections saying the exact same thing.

Both analyzers registered via one additional `@Binds`/`@IntoSet` line
each in the same `AnalysisEngineBindingModule` — zero changes needed to
`ThreatAnalyzerRegistry`, `AnalyzeScanTargetUseCase`, or
`AnalyzerExecutor`, exactly the plug-in property those classes were
designed around since Sprint 004C/006.

```
RunScanRequestUseCase
   │
   ▼
AnalyzeScanTargetUseCase → registry.analyzersFor(ApplicationTarget)
   │                              (now returns 2 analyzers)
   ├─▶ SuspiciousPermissionPatternAnalyzer   ─┐  (concurrent,
   └─▶ AppIdentityImpersonationAnalyzer      ─┘   fault-isolated)
         │
         ▼
   AnalysisOutcomeAggregator.aggregate()
         │  concatenate → deduplicate exact matches → Flagged | Clean
         ▼
```

See ADR 0028 for the full reasoning, including a real regression this
sprint's own self-review caught: an existing test fixture that generated
identical placeholder evidence text across detections would have been
silently collapsed by the new dedup logic, breaking a load-bearing
existing test if left unfixed.

### Production ThreatDescriptionProvider — and a governance document that never existed (Sprint 016)

Before writing any copy, a check for Sprint 002.75's source document
(cited by section number — §4 through §21 — across 30+ files in this
codebase) found it was never committed to this repository at all. Fixed,
not just flagged: `docs/content-style-guide.md` consolidates every
consistently-applied rule those citations imply into a real, checkable
artifact — the first time "follow Sprint 002.75's guidance" has meant
something verifiable rather than an unrecoverable external reference.
Its own provenance note says plainly it's a reconstruction, to be
superseded if the real document is ever located.

`ProductionThreatDescriptionProvider` (`core:analysisengine`) is written
against it: short static titles that never claim the verdict; a
description that always shows every `Detection`'s evidence (not just
ones matching the "driving" `threatType` `BuildThreatUseCase` passes in
— a `Threat` combining two analyzers' findings must show both, not just
whichever one determined the headline category); a suggested action
phrased as something to consider, never a demand, matching
`RiskLevel.ATTENTION`'s own epistemic humility. Covers all four
`ThreatType` values, including two (`MALWARE`, `UNKNOWN`) no analyzer
currently produces.

Bound in `AnalysisEngineBindingModule` — the last binding ADR 0026 left
open. `RunScanRequestUseCase` is fully Hilt-constructible for the first
time in this project's history; `AnalysisEngineBindingModuleTest` now
injects it directly to prove that, where every earlier version of that
test deliberately avoided the attempt because it would have failed.

## Phase C: UI Architecture

### The first production screen (Sprint 017)

Sixteen sprints of domain/data work, zero user-visible output — until
this sprint. `feature:home`'s Sprint 003 placeholder is replaced with a
real screen, and the pattern it establishes is meant to be followed
exactly by every remaining feature screen, not just this one:

```
FeatureRoute (stateful)          FeatureScreen (stateless)
  hiltViewModel()          →       pure function of FeatureUiState
  collectAsStateWithLifecycle()    no ViewModel/DI awareness at all
                                    → physically cannot hide business logic
```

`HomeViewModel` combines two Flow-based UseCases
(`ObserveScanHistoryUseCase`, `ObserveTrustedItemsUseCase`) via
`combine()` → `stateIn(WhileSubscribed(5_000))` — reactive by
construction, the current standard Android lifecycle-aware state
pattern. `GetLatestScanResultUseCase`/`GetActiveScanSessionUseCase` both
exist but are deliberately unused here: one-shot suspend calls are a
worse fit than deriving "last scan" from the same reactive Flow that
already exists.

**Testing pattern established:** `mockk` on `SecurityRepository`/
`TrustedItemRepository` (stubbed only for the methods actually called)
fed into real UseCase instances — not local hand-written `Fake*` classes
(those exist only in `:domain`'s own test source set, invisible
downstream) — proportionate for a ViewModel exercising 2 of 18 combined
repository methods. Compose UI tests use `createComposeRule()` against
the stateless `FeatureScreen` directly, needing no Hilt test
infrastructure at all, for the same reason the stateful/stateless split
matters.

See ADR 0030 for the full reasoning, including a real bug caught while
writing the ViewModel tests (`stateIn`'s `initialValue` means every new
collector sees `Loading` first, as a genuinely separate emission from the
real state — every test needed to consume it explicitly), and an
explicit note that `OnboardingNavigationRoute` — itself still Sprint
003's placeholder — remains the app's actual navigation start
destination; that wasn't in this sprint's scope to change.

### Production onboarding, and closing a gap in Sprint 017's reported fixes (Sprint 018)

Before starting this sprint, a check of the actual pushed `main` branch
found that Sprint 017's two reported "compile-only compatibility
corrections" (a missing `getValue` import, `ErrorOutline` → `Warning`)
had not actually landed in the file that needed them — `HomeScreen.kt`.
The `getValue` import instead landed in `SpaceAntivirusNavHost.kt`, a
file Sprint 017 never touched. Both fixes since landed in a separate
main commit ahead of this sprint's own patch. See ADR 0031 for the full
account.

`OnboardingScreen` (`feature:onboarding`) replaces Sprint 003's
placeholder, following ADR 0030's stateful/stateless split exactly.
Given what just happened with `ErrorOutline`, this screen deliberately
uses **zero** Material icons anywhere — the safest way to guarantee no
repeat of that exact mistake with no real compiler available to verify
against. Static page content (`OnboardingContent.kt`) is its own file,
owned by neither the ViewModel nor the Screen, so a future page is one
list entry, no other file touched.

```
OnboardingContent.kt (OnboardingPages: List<OnboardingPage>)
        │                                    │
        ▼                                    ▼
OnboardingViewModel                   OnboardingScreen
  (bounds-checked page index)           (renders current page,
                                          Next/Back/Get Started)
```

Wired into `SpaceAntivirusNavHost`: completing onboarding navigates to
Home with `popUpTo(OnboardingNavigationRoute) { inclusive = true }` — no
back-navigation into onboarding once past it.

Onboarding copy is deliberately scoped to what the app actually does
today (installed-application permission and identity checks, Sprints
014/015) and explicitly states what it doesn't do yet (file/message/photo
scanning, real-time monitoring) — no overpromising relative to the real
pipeline.

### Security Center (Sprint 019)

No new pattern — follows ADR 0030 exactly. Reuses `ObserveScanHistoryUseCase`
(the same Flow `HomeViewModel` already uses) to surface the *full*
`Threat` list from the latest scan, not just a count — real titles and
descriptions from Sprint 016's `ProductionThreatDescriptionProvider`, the
first screen where a specific finding is actually readable.

Two deliberate content-allocation choices: trusted items are NOT shown
here (already on Home, no security-specific value in repeating it), and
`ProtectionStatus` is duplicated rather than shared between
`feature:home`/`feature:security` — feature modules don't depend on each
other in this project, and a five-line `when` expression doesn't justify
a new shared module yet (rule of three).

`RiskLevel` → `Severity` mapping lives in `SecurityCenterScreen.kt`, not
the ViewModel — `RiskLevel`'s own KDoc already says `core:ui`'s
`Severity` should map onto it, and keeping the mapping in the Screen
keeps the ViewModel free of any `core:ui` import.

Same recurring bug caught again during self-review, worth naming as a
pattern rather than a one-off: every new `stateIn`-based ViewModel test
needs to explicitly consume the initial `Loading` emission before
asserting on real state — first documented in ADR 0030, rediscovered and
fixed here too. See ADR 0032.

### First scan execution UI (Sprint 020)

A real architectural gap was found and reported before writing any
bridging code: `RunScanRequestUseCase` doesn't expose the session id it
creates until the whole scan finishes, but `ObserveScanProgressUseCase`
needs that id immediately to observe live progress. No existing single
reactive path connects "start a scan" to "watch its progress."

Bridged, not redesigned: `ScanViewModel` polls `GetActiveScanSessionUseCase`
(bounded, 50ms × 20 attempts) immediately after triggering the scan,
switching to `ObserveScanProgressUseCase(id)` once found. Session creation
is a single early Room insert, so the window is generous. If the poll
times out, progress observation is skipped and the scan's own result
still reaches the UI normally — redesigning `RunScanRequestUseCase`'s
signature (spanning Sprints 005–016, heavily tested) was judged out of
proportion for one UI sprint. See ADR 0033 for the full reasoning.

```
ScanViewModel.startScan()
   │
   ├─▶ async { RunScanRequestUseCase(request) }  ─────────┐
   │                                                        │
   └─▶ launch {                                             │
         val session = awaitActiveSession()  (bounded poll) │
         session?.let {                                     │
           ObserveScanProgressUseCase(it.id).collect { ... }│  (cancelled once
         }                                                   │   the scan
       }                                                     │   completes)
                                                              ▼
                                                    ScanUiState.Completed
```

A separate `ScanViewModel`, not folded into `HomeViewModel` — passive
status observation and active scan orchestration are different concerns.
No dialog used for progress/results: `AppConfirmDialog` is explicitly
documented as reserved for blocking confirmations only, never
informational content — both render inline on Home instead. No
duplicate results detail either: `ScanUiState.Completed` carries only a
brief summary; Security Center (Sprint 019) already reactively shows
full per-threat detail from the same data this scan persists into.

A real test-infrastructure gotcha caught while testing the double-trigger
guard: `MainDispatcherRule` uses `StandardTestDispatcher`, which does NOT
run `launch{}` immediately the way production's `Dispatchers.Main.immediate`
does — three back-to-back `startScan()` calls with no `runCurrent()`
between them would have silently verified nothing. Fixed by advancing the
dispatcher explicitly between calls.

### Scan Results / History (Sprint 021)

`feature:history` was still Sprint 003's literal placeholder text — the
one genuinely unbuilt "results" screen, distinct from Security Center
(which already shows the *latest* scan's detail). `HistoryViewModel`
reuses `ObserveScanHistoryUseCase` unmapped: every completed scan, not
just the first, in the same most-recent-first order Sprint 010's query
already provides. No new UseCase needed.

A second real gap was found before building anything: History was
unreachable anywhere in the real app — deliberately not one of the 4
bottom-nav tabs (`TopLevelDestination`'s own KDoc), and nothing linked to
it. Fixed with a "View full history" entry point on Security Center,
reusing the same callback-based navigation pattern already established
for onboarding completion (Sprint 018) — `onViewHistoryClick` threaded
through `SecurityCenterRoute`, wired in `SpaceAntivirusNavHost`.

```
ObserveScanHistoryUseCase() — the same Flow all three screens now share
   │
   ├─▶ HomeViewModel          — .firstOrNull(), compact summary
   ├─▶ SecurityCenterViewModel — .firstOrNull(), full threat detail
   └─▶ HistoryViewModel        — the full list, every completed scan
```

A scan completing anywhere in the app is reflected consistently and
immediately across all three screens with zero additional wiring — a
direct payoff of the reactive-Flow architecture established since Sprint
017, not something this sprint had to build specially.

Each History entry shows its own metadata and, for scans with findings,
every threat inline — no separate detail screen, since History already
shows everything a detail screen would.

### Junk-file domain logic — Phase C's last domain gap (Sprint 022)

Before writing any code, current `main` was checked directly against the
original Phase C roadmap rather than assumed: five of six planned pieces
were done (under evolved scope/numbering — Home, Onboarding, Security
Center, Scan Execution, Scan Results/History), but junk-file domain logic
never existed at all, and `feature:clean` was still Sprint 003's literal
placeholder. This is that gap.

`FindCleanableItemsUseCase` reuses `EnumerationRepository.enumerateFiles`
(Sprint 004B) directly — no new repository, no new Hilt module. Genuinely
separate from `ThreatAnalyzer`/`RunScanRequestUseCase`: a cache file is
not a security concern, and `CleanableItem`/`CleanableCategory` are new,
standalone models, not variants of `Threat`/`ThreatType`.

```
EnumerationRepository.enumerateFiles(scope)  — the same contract
   │                                            RunScanRequestUseCase's
   │                                            target resolution uses
   ▼
FindCleanableItemsUseCase
   │  mapNotNull { JunkFileClassifier.classify(file, now) }
   ▼
List<CleanableItem>
```

`JunkFileClassifier` — four conservative, evidence-based rules, same
discipline as Sprints 014/015's analyzers: `CACHE_FILE` (a `/cache/` path
segment — safe by Android convention), `TEMPORARY_FILE` (a small closed
extension set), `LOG_FILE` (`.log`), `LEFTOVER_INSTALLER` (a `.apk` in
Downloads, unmodified for 24+ hours — the age requirement specifically
avoids flagging an installer the user just downloaded). `nowEpochMillis`
is an explicit parameter, never read internally, keeping the age-based
rule fully deterministic and testable.

Candidates only — nothing in this domain layer deletes a file. That's
explicitly Clean UI's job, once this layer exists for it to act on. See
ADR 0035 for the full reasoning.

### Clean screen — genuinely closing Phase C (Sprint 023)

After Sprint 022, verification against current `main` found Phase C
wasn't actually complete: `feature:clean` was still Sprint 003's literal
placeholder, and nothing referenced `FindCleanableItemsUseCase` outside
`domain/`. Confirmed with the user before proceeding rather than assumed
either way.

`CleanViewModel` is action-triggered, not Flow-combine — same shape as
`ScanViewModel` (Sprint 020), for the same reason: `FindCleanableItemsUseCase`
is a one-shot file enumeration, not backed by an ongoing observable Flow
the way persisted scan history is.

**Display-only, deliberately.** `CleanableItem` was scoped (ADR 0035) as
candidates only — no delete-capable UseCase or repository method exists
anywhere in this project. Building a delete button that doesn't delete
anything would be fake production code; real file deletion is separate,
future domain work.

```
FindCleanableItemsUseCase(ScanScope.InternalStorage)  — user-triggered,
   │                                                      one-shot
   ▼
CleanUiState.Loading → Loaded(items, totalSizeBytes) | Error
```

Fixed to `ScanScope.InternalStorage` for now — the UseCase already takes
`ScanScope` as a parameter, so scope selection is a future, additive UI
change, not a redesign. No navigation wiring needed: unlike History,
Clean is already one of the 4 bottom-nav tabs, reachable since Sprint
003. See ADR 0036.

With this sprint, Phase C is genuinely complete — all six original
roadmap items are real production screens.

## Phase D: Background Protection

### Background scan infrastructure (Sprint 024)

The smallest real, working slice of Phase D: schedule and run a
background scan reusing the existing production pipeline. No
notifications, no quarantine, no real-time monitoring — all explicitly
out of scope for this increment.

```
BackgroundScanScheduler (domain contract, pure Kotlin)
   │
   ▼
WorkManagerBackgroundScanScheduler (core:workmanager — the ONLY class
   │                                 in this project touching WorkManager)
   ▼
PeriodicWorkRequest<ScanWorker>  — every 24h, battery-not-low only
   │
   ▼
ScanWorker.doWork()
   │  builds a ScanRequest(InstalledApplications, QUICK) — same scope/
   │  type a manual "Scan Now" tap uses (ScanViewModel, Sprint 020)
   ▼
RunScanRequestUseCase(request)  — the SAME real pipeline, no second
                                   implementation
```

`ScanWorker` is a `CoroutineWorker` (not the older callback-based
`Worker`) — that alone is what "lifecycle-safe execution" required;
nothing extra was built for it. Its `AppResult → WorkManager.Result`
mapping is reasoned per branch: success stays success;
`ScanAlreadyInProgress` (the existing concurrent-scan guard, ADR 0020,
now applying automatically to background scans too) also maps to
success, since the guard doing its job isn't this worker failing;
`PermissionMissing` maps to `Result.failure()` since retrying can't fix
a missing permission; everything else defers to `Result.retry()`.

**Deliberately not wired to activate automatically.**
`ScheduleBackgroundScanUseCase` is real and fully tested, but nothing in
this sprint calls it — no Settings toggle exists yet to let a user opt
in, and silently enabling background scanning for every install without
that decision isn't this sprint's call to make. The next increment is a
small, additive UI change against infrastructure that already works, not
a redesign.

One real, explicitly-flagged API-surface uncertainty: `SpaceAntivirusApp`
now implements `Configuration.Provider` (`workManagerConfiguration` as a
Kotlin property) so WorkManager's default initializer picks up
`HiltWorkerFactory` automatically — this project's pinned WorkManager
version should support the property form, but this couldn't be verified
against a real compiler in this sandbox. If wrong, it's a single-line,
isolated compatibility fix, not an architectural one.

See ADR 0037 for the full reasoning behind every decision in this
sprint, including the testing approach (real `TestListenableWorkerBuilder`
and real test-mode `WorkManager`, not mocks of either — the same
"prefer real infrastructure" discipline established since Sprint 010).

### Configurable scheduling and boot-lifecycle persistence (Sprint 025)

Two real gaps closed, both confirmed against the repository before
writing code, not assumed. First: `schedulePeriodicScan`'s interval was a
hardcoded implementation constant — now an explicit `intervalHours`
parameter (`BackgroundScanScheduler.DEFAULT_INTERVAL_HOURS = 24`,
`MIN_INTERVAL_HOURS = 1`, validated with a new
`AppError.InvalidScheduleConfiguration` rather than trusting WorkManager's
own internal clamping). `ScheduleBackgroundScanUseCase` changed from
`NoParamsUseCase<Unit>` to `UseCase<Long, Unit>` to carry it. Still
deliberately not wired to activate — the same "foundation, not
activation" discipline ADR 0037 established, extended one layer further.

Second: `RECEIVE_BOOT_COMPLETED` is now declared in the manifest — without
it, a scheduled periodic scan would silently stop firing after any device
reboot until the user happened to relaunch the app. No custom
`BroadcastReceiver` was written; WorkManager's own bundled boot-
rescheduling component (merged automatically from its AAR) handles
re-registering already-persisted work once the permission is present —
writing one anyway would have been exactly the "duplicate scheduling
logic" this sprint's own instructions prohibited.

Also added: `setRequiresStorageNotLow(true)` (general background-work
hygiene, not specific to scan content) and explicit `setBackoffCriteria`
(`EXPONENTIAL`, `WorkRequest.MIN_BACKOFF_MILLIS`) — making a previously-
implicit WorkManager default visible and intentional.

See ADR 0038 for the full reasoning, including confirming directly (not
assuming) that adding a new `AppError` case couldn't break any existing
exhaustive `when` in the codebase before doing so.

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
