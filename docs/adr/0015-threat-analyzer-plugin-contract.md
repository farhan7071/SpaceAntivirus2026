# ADR 0015: Threat Analyzer plug-in contract, and Detection's provenance field

**Status:** Accepted

## Context
Sprint 004C's mandate is specific: build the architecture that lets *any*
future detection engine (signature, heuristic, AI/ML, cloud-reputation,
behavioral) plug in without ever requiring a change to `domain` itself.
That's a Strategy/plug-in pattern requirement, not a data-modeling
requirement — the real design question this sprint had to answer is
"what's the seam", not "what are the fields".

Sprint 004A's `Detection` model (id, threatType, evidenceDescription,
riskLevel) has no way to record which analyzer produced a given piece of
evidence. That was fine when only one conceptual "engine" existed
implicitly; it stops being fine the moment multiple analyzer types can
each contribute Detections against the same target — without provenance,
a future results screen (or a bug report, or a false-positive dispute)
can't say *which* engine claimed what, which undermines the "always show
evidence" principle (Sprint 002.75 §17) at exactly the level of detail
that principle is about.

## Decision
1. **`ThreatAnalyzer`** (domain/analyzer): the plug-in contract itself —
   `id`, `capabilities: Set<AnalyzerCapability>`, and
   `suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome>`.
   Deliberately minimal — one target in, one outcome out — so it stays
   stable as new analyzer types are added. Orchestration (running multiple
   analyzers, aggregating their outcomes) is explicitly NOT this
   interface's job; that's a future UseCase's job, composing multiple
   `ThreatAnalyzer` instances.
2. **`ThreatAnalyzerRegistry`** (domain/analyzer): contract for
   discovering which analyzers exist and which apply to a given target.
   No implementation in this sprint (the "repository interfaces remain
   abstractions" rule applies equally here) — the likely future
   implementation uses Hilt `@IntoSet` multibindings, but this contract
   doesn't commit to that.
3. **`RiskScorer`** (domain/scoring): a second, smaller plug-in point —
   how a set of Detections reduces to one overall `RiskLevel`. Unlike
   `ThreatAnalyzer`/`ThreatAnalyzerRegistry`, this sprint DOES ship a
   concrete default (`HighestSeverityRiskScorer`: the max severity among
   all Detections) — this is scoring/summarization of already-found
   evidence, not detection logic deciding whether something is a threat,
   so it doesn't cross into "no shortcut/fake detection" territory. It's
   also the most defensible possible default per Sprint 002.75 §17
   ("never exaggerate risk") — reporting the worst already-found thing,
   nothing invented or weighted.
4. **`Detection.analyzerId: AnalyzerId`** — added as a required field, not
   an optional/defaulted one. This is a deliberate breaking change to an
   already-shipped (Sprint 004A) model. The one existing call site
   (`CompleteScanSessionUseCaseTest.kt`) was updated in this same patch,
   not left broken.

## Consequences
- Adding a new detection engine in a future sprint means: implement
  `ThreatAnalyzer` in a new module, give it a stable `AnalyzerId`, bind it
  into the registry. Nothing in `domain`, `core:model`, or any existing
  analyzer's code needs to change — this is the concrete verification that
  Sprint 004C's stated goal ("any future scanner can plug in without
  modifying existing domain code") is actually achievable with what's
  here, not just asserted.
- Every `Detection` from this point forward carries real provenance. Any
  future analyzer that can't honestly attribute its own findings (e.g. a
  cloud-reputation call whose actual verdict came from a third party) still
  must supply *an* `AnalyzerId` identifying itself as the Detection's
  source within this app — attribution to the calling analyzer, not
  necessarily the ultimate origin of the underlying data.
- `RiskScorer` being an interface (not just `HighestSeverityRiskScorer`
  directly) means a future sprint introducing analyzer-confidence
  weighting, for example, can add a new `RiskScorer` implementation and
  swap it in via DI without touching whatever calls `RiskScorer.score(...)`.
- This is the second time in this project a model needed a breaking change
  after shipping (`AppError` was extended, non-breaking, in ADR 0013;
  this is the first actually-breaking one). Worth stating as a standing
  expectation: Sprint 004A/004B's models were reasonable given what was
  known then, and revising them under a documented ADR when new
  requirements arrive is the correct response — not a sign the earlier
  work was wrong.
