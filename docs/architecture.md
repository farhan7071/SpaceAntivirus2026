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
