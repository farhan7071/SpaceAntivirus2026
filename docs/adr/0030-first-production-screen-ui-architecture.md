# ADR 0030: First production screen — the UI architecture pattern every remaining screen follows

**Status:** Accepted

## Context
Sprint 017 is the first UI work in this entire project — sixteen sprints of domain/data-layer work with zero user-visible output until now. Its explicit purpose was not just "build the Home screen" but "establish the UI architecture that the remaining feature screens will follow." Every decision here is a precedent, not a one-off.

## Decisions

### 1. Stateful/stateless split: `HomeRoute` vs `HomeScreen`
`HomeRoute` is the only composable that touches `hiltViewModel()`/`collectAsStateWithLifecycle()`. `HomeScreen` is a pure function of `HomeUiState`, with no ViewModel or DI awareness at all. This isn't just a style preference — it's what makes "no business logic inside Compose" enforceable rather than aspirational: `HomeScreen` is physically incapable of reaching a UseCase, a repository, or anything else it shouldn't touch, because it never receives a reference to any of them. It's also what made a real Compose UI test possible without any Hilt test infrastructure (see #4).

### 2. Reactive by construction, not by requirement
`HomeViewModel` combines two Flow-based UseCases (`ObserveScanHistoryUseCase`, `ObserveTrustedItemsUseCase`) via `combine()`, exposed as a single `StateFlow` via `stateIn(WhileSubscribed(5_000))` — the current standard Android architecture pattern for lifecycle-aware state: upstream collection starts only when there's a UI subscriber and stops shortly after the last one goes away, surviving brief configuration-change gaps without needless re-subscription.

**`GetLatestScanResultUseCase` and `GetActiveScanSessionUseCase` both exist and were deliberately not used here.** They're one-shot suspend calls; "last scan" is more genuinely reactive derived from `ObserveScanHistoryUseCase`'s own Flow — it updates live the moment a new scan completes while Home is open, not just on next ViewModel creation. Active-scan-session state is not surfaced at all yet, since this sprint explicitly excludes scan execution/progress UI — there's nothing yet for that signal to drive.

### 3. `ProtectionStatus` derivation lives in the ViewModel, not the Composable
`PROTECTED` / `NEEDS_ATTENTION` / `UNKNOWN` is computed from the latest `ScanResult` in `HomeViewModel`, not inferred inline in `HomeScreen`. `UNKNOWN` deliberately covers both "never scanned" and "last scan didn't reach `COMPLETED`" — both are genuinely "we don't know," not a lesser form of `PROTECTED`. The `state != COMPLETED` branch is currently unreachable given how `observeScanHistory()`'s real query is written (Sprint 010 filters to `COMPLETED` only) — kept anyway as a defensive check against a `ScanResult`'s type not itself guaranteeing that invariant, and against a future implementation weakening it unnoticed.

### 4. Testing: mockk on repositories through real UseCases, not local Fake* classes
`domain`'s own `Fake*` test doubles exist only in `:domain`'s own test source set — invisible to `feature:home`'s tests regardless of visibility modifiers, since Gradle test source sets are never exposed to consuming modules. Writing full local `Fake` implementations of `SecurityRepository` (14 methods) and `TrustedItemRepository` (4 methods) just to exercise the 2 methods `HomeViewModel` actually calls would be disproportionate boilerplate. `mockk<SecurityRepository>()`/`mockk<TrustedItemRepository>()`, stubbed only for the methods actually invoked, fed into real `ObserveScanHistoryUseCase`/`ObserveTrustedItemsUseCase` instances, is the proportionate choice — establishing the pattern for feature ViewModel tests going forward, distinct from `domain`'s own hand-written-Fake convention.

A real bug was caught while writing these tests: `stateIn`'s `initialValue = Loading` means the `StateFlow`'s first emission to any new collector is always `Loading`, before the real combined value arrives as a second, separate emission — every test needed to consume that first emission via Turbine before asserting on the real state, or the cast to `HomeUiState.Loaded` would fail.

### 5. Compose UI testing: `createComposeRule()` against the stateless composable directly
This project's first Compose UI test. No Hilt test infrastructure needed, for the same reason the stateful/stateless split matters (#1): `HomeScreenTest` constructs `HomeUiState` by hand and renders `HomeScreen` directly, never `HomeRoute`. Wrapped in `SpaceAntivirusTheme` to match how the screen is actually rendered in the real app (not because any component would crash without it — `LocalSpacing` and `MaterialTheme` both have built-in defaults). A `testTag` was added to the loading indicator specifically because it's the one state with no visible text to assert on.

`spaceav.android.feature`'s convention plugin doesn't wire `androidTestImplementation(compose-ui-test-junit4)` or a `testInstrumentationRunner` — the same gap found and fixed individually in `core:database` (Sprint 010), `core:securitydata`/`core:trusteddata` (Sprint 011/012). Added directly to `feature:home/build.gradle.kts`; every other feature module will need the same until/unless it's promoted into the shared convention plugin.

## Consequences
- Every remaining feature screen (Security Center, Clean, Settings, History, etc.) should follow this exact shape: `FeatureRoute` (stateful, ViewModel-aware) + `FeatureScreen` (stateless, pure function of a sealed `FeatureUiState`) + a ViewModel combining Flow-based UseCases via `stateIn(WhileSubscribed(5_000))` + mockk-on-repository ViewModel tests + `createComposeRule()` UI tests against the stateless composable.
- `feature:home` is the first feature module with real business-adjacent logic (`protectionStatusFor`) inside a ViewModel — worth checking in future ViewModel reviews that this logic stays proportionate to "deriving UI state from domain data" and doesn't grow into business logic that belongs in `domain` instead.
- **Not addressed by this sprint, worth naming explicitly:** the app's actual navigation start destination remains `OnboardingNavigationRoute` (still Sprint 003's placeholder, unchanged) — launching the app today shows Onboarding first, then Home once navigated past it or reached via the bottom bar. This sprint's success criterion ("Home is no longer a placeholder") is fully met; it does not mean Home is the very first screen a cold launch shows, and that wasn't in this sprint's scope to change.
