# ADR 0017: ScanStatistics tracks inconclusive results; RunScanRequestUseCase ties enumeration, analysis, and persistence together

**Status:** Accepted

## Context
Building the orchestration UseCase that ADR 0016 named as Sprint 004C's
one remaining piece of real future work (connecting enumeration, analysis,
and persistence end to end) surfaced a real gap in an already-shipped
model before any of that orchestration could be written honestly.

`ScanStatistics` (Sprint 004A) had `itemsScanned` and `threatsFound`, with
no way to represent "N targets were analyzed but no analyzer could reach
a conclusion." `AnalysisOutcomeAggregator` (Sprint 004C) was deliberately
designed so `Inconclusive` beats `Clean` — the app must never claim "no
threats found" when part of an analysis didn't reach a real conclusion.
Given the project's actual current state (no `ThreatAnalyzerRegistry`
implementation exists yet, so every real scan today would return
`Inconclusive` for every target), writing an orchestration UseCase against
the old `ScanStatistics` shape would have silently thrown that Sprint
004C design principle away at the very last step — a `ScanResult` with
`threatsFound=0` and no way to say "and none of this was actually checked"
is indistinguishable from a genuinely clean scan.

## Decision
1. Added `itemsInconclusive: Int` to `ScanStatistics` as a required field
   (not optional/defaulted) — same breaking-change precedent as ADR
   0015's `Detection.analyzerId`. Updated the invariant (`threatsFound +
   itemsInconclusive <= itemsScanned`) and all existing call sites (three
   test files) in this same patch.
2. Fixed `ScanResult.isClean` to also require `itemsInconclusive == 0`.
   Previously it only checked `threats.isEmpty()`, which had the same
   false-reassurance problem one level up.
3. Added `RunScanRequestUseCase` (domain/usecase): the top-level
   orchestration — resolves a `ScanRequest`'s targets (Sprint 004B),
   analyzes each one (Sprint 004C), builds `Threat`s from `Flagged`
   outcomes, tracks `Inconclusive` counts honestly, and persists the
   result via Sprint 004A's `StartScanSessionUseCase`/
   `CompleteScanSessionUseCase`. This is the concrete, testable proof
   that Sprints 004A, 004B, and 004C's independently-built contracts
   actually compose into one working pipeline.

## Consequences
- Today, with no real `ThreatAnalyzer` bound anywhere, running
  `RunScanRequestUseCase` against a real device would produce a
  `ScanResult` with `itemsInconclusive` equal to `itemsScanned` and
  `isClean == false` — an honest "we looked, but nothing is actually
  checking yet" result, not a false "you're protected." This is the
  correct, if unglamorous, current truth of the system, and it's better
  that the architecture surfaces it accurately than hides it.
- Any future UI built against `ScanResult` needs a real state for "mostly
  or entirely inconclusive" (distinct from both "clean" and "threats
  found") — not designed in this patch (no UI changes are in scope), but
  worth flagging now so it isn't a surprise when that screen gets built.
- A partial-failure policy (what happens if one target's analyzer throws
  partway through a large scan) is currently fail-fast — the first
  `AppResult.Failure` aborts the whole `RunScanRequestUseCase` call. A
  more resilient policy (skip and continue, report per-target errors)
  is real future work, not implemented here to avoid inventing retry/
  partial-result semantics without a concrete case driving the design.
