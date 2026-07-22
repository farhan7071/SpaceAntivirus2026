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
| `core:model` | Shared data classes need one home so `core:data` and every `feature:*` agree on the same type without a feature depending on another feature. Empty in Sprint 003 by design — see the module's own KDoc. |
| `core:designsystem` | Owns Material 3 tokens (color/type/shape/spacing) — separated from `core:ui` (components) so a future rebrand touches one module, not every component. |
| `core:ui` | Reusable Compose components (Sprint 002.5 §9). Kept separate from `core:designsystem` because components change more often than tokens do, and feature authors should import "the button," not "the whole theme." |
| `core:data` | Repository implementations and DataStore-backed preferences — the seam between "business logic" (`domain`) and "how data is actually stored" (`core:database`, `core:network`). |
| `core:database` | Room. Isolated so swapping persistence technology later (unlikely, but possible) doesn't ripple into `core:data`'s Repository interfaces. |
| `core:network` | Retrofit/OkHttp/retry policy. Isolated for the same reason as `core:database`, and because the eventual OTA signature-update pipeline (Sprint 002 §8) will be the first real consumer of this module. |
| `core:security` | Encrypted storage and crypto/Play-Integrity abstractions — deliberately narrow-scoped (ADR 0008) so "sensitive value" stays a meaningful, reviewable category. |
| `core:permissions` | Sprint 001 Risk #4 (permission sprawl) is addressed architecturally here: `AppPermission` is a closed enum, not a place to pass arbitrary permission strings. |
| `core:testing` | Shared test doubles/rules (`MainDispatcherRule`, `TestDispatchers`) so every module's test setup is identical, not reinvented per feature. |
| `domain` | UseCases that coordinate more than one Repository call — kept as pure Kotlin (ADR 0005) so business logic is unit-testable without any Android dependency. |
| `feature:*` (9 modules) | One per Sprint 002.5 screen area. Never depend on each other (ADR 0004) — this is the direct fix for Sprint 001's "engine and shell tightly interwoven" finding: a feature module can be rewritten or even deleted without touching another feature. |
| `benchmark` | Macrobenchmark module, isolated per AndroidX convention (it needs `com.android.test`, a different plugin from every other module). |
| `build-logic` | Composite build holding the `spaceav.android.*` convention plugins — keeps every module's own `build.gradle.kts` to ~10 lines instead of repeating compileSdk/Compose/Hilt setup 20 times. |

## Data flow example (illustrative — no real feature exists yet)

Once Sprint 004 adds the scan feature, the intended flow is:

```
ScanScreen (Compose)
   │ collects
   ▼
ScanViewModel (StateFlow<ScanUiState>)
   │ calls
   ▼
RunScanUseCase (domain) — coordinates:
   │
   ├──▶ ScanRepository (core:data) ──▶ core:database (persist result)
   └──▶ ScanRepository (core:data) ──▶ [malware engine — integration TBD,
                                        see Sprint 002 §14 open question #2]
```

Every arrow above returns `AppResult<T>` (ADR 0007), so `ScanUiState` can be
a simple `sealed interface` derived directly from that result without a
separate error-mapping step at the ViewModel layer.

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
