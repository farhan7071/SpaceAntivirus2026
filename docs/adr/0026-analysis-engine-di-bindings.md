# ADR 0026: Closing the analysis-side Hilt DI gaps — and what's still deliberately open

**Status:** Accepted

## Context
A Sprint 012 status review found two real, evidence-based gaps in the Hilt graph: `ThreatAnalyzerRegistry` and `RiskScorer` both had concrete implementations (`DefaultThreatAnalyzerRegistry` since Sprint 006, `HighestSeverityRiskScorer` since Sprint 004C) that were never bound to their interfaces anywhere, and `HighestSeverityRiskScorer` didn't even have an `@Inject` constructor — Hilt couldn't have constructed it under any circumstances. Neither gap had ever caused a build failure only because nothing in `feature/` or `app/` yet injects anything downstream of them (no feature ViewModel exists with real logic — every feature module remains the Sprint 003 placeholder). The first real ViewModel to inject the scan pipeline would have hit a hard compile-time Hilt error.

## Decisions

### 1. New module: `core:analysisengine`
`:domain` cannot host `@Module`/`@Binds`/`@Multibinds` declarations itself — it's pure Kotlin with no Hilt/KSP processing (ADR 0005/0011). Unlike `core:securitydata`/`core:trusteddata`, there's no new Room-backed implementation class to write here — `DefaultThreatAnalyzerRegistry` and `HighestSeverityRiskScorer` already exist, entirely in `:domain`. This module's only job is to be the Hilt-enabled home for wiring them into the graph. It's also the natural home for future `ThreatAnalyzer` implementations to declare their `@IntoSet` contributions once Phase A adds real analyzers.

### 2. `@Multibinds` for the empty `Set<ThreatAnalyzer>` case
`DefaultThreatAnalyzerRegistry`'s constructor requests `Set<ThreatAnalyzer>`. Without an explicit `@Multibinds` declaration, Dagger has no way to distinguish "this Set is legitimately empty" from "no binding exists" — it would fail graph validation entirely. `@Multibinds abstract fun bindThreatAnalyzers(): Set<@JvmSuppressWildcards ThreatAnalyzer>` declares the empty case as valid now; the first real analyzer contributes via `@IntoSet` in its own module later, with no change needed here.

### 3. A real instrumented Hilt-graph test, not just static reasoning
Every other Room-adjacent decision in this project has been verified by a real instrumented test because mocking would have been fragile or misleading (ADR 0023/0024/0025). The same principle applies here for a different reason: whether `@Binds`/`@Multibinds` declarations actually satisfy Dagger's compile-time graph validation is not something a JVM unit test can check at all — it requires Hilt's own annotation processor to run against the real, fully-assembled `:app` component. `AnalysisEngineBindingModuleTest` (`app/src/androidTest`) is a genuine `@HiltAndroidTest`, using a new `HiltTestRunner` (launching `HiltTestApplication`) to inject `ThreatAnalyzerRegistry` and `RiskScorer` from the real graph and exercise both — including confirming the empty-Set case resolves without error, not just that it compiles.

### 4. `ThreatDescriptionProvider` remains explicitly, deliberately unbound
Flagged before any code was written this sprint, not discovered partway through: closing `ThreatAnalyzerRegistry`/`RiskScorer`'s bindings does **not** make `BuildThreatUseCase` (or therefore `RunScanRequestUseCase`) fully Hilt-constructible, since `ThreatDescriptionProvider` still has no implementation (ADR 0016 — that copy needs Sprint 002.75's content-governance review). Binding it to a stub now would be exactly the kind of workaround this project's standing rules prohibit introducing instead of reporting. `AnalysisEngineBindingModuleTest`'s own scope is written to match this precisely — it does not attempt to inject anything downstream of `ThreatDescriptionProvider`.

## Consequences
- The two gaps found in the Sprint 012 status review are closed, verified by a real instrumented test, not asserted.
- `AnalysisEngineBindingModule` is the pattern the first real `ThreatAnalyzer` (a later Phase A sprint) will extend — add an `@IntoSet`-annotated provider in its own module, no change needed to this one.
- `RunScanRequestUseCase`/`BuildThreatUseCase` remain not fully Hilt-constructible until `ThreatDescriptionProvider` gets a real, governed implementation — Phase B's explicit job, not silently absorbed into this sprint.
- No `domain` behavior changed — `HighestSeverityRiskScorer`'s only change is the addition of `@Inject` to its constructor, a pure DI-wiring fix with no effect on its existing, already-tested `score()` logic.
