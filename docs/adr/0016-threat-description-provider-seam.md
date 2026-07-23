# ADR 0016: ThreatDescriptionProvider is a contract with no default implementation in domain

**Status:** Accepted

## Context
Sprint 004C Patches 1 and 2 both explicitly deferred one piece: converting
an `AnalysisOutcome.Flagged` into a persistable `Threat`, because `Threat`
requires `title`/`description` fields, and `domain` has no legitimate way
to generate that copy itself. Sprint 002.75 established a full
content-governance process (Vocabulary Dictionary §4, Security Messaging
Guide §7, Content Governance §20) specifically so user-facing security
copy doesn't get written ad hoc under implementation pressure. Writing
plausible-sounding title/description strings directly inside `domain` —
even carefully worded ones — would route around that process entirely.

## Decision
`ThreatDescriptionProvider` (domain/reporting) is a contract only:
`titleFor(threatType, detections)` / `descriptionFor(threatType,
detections)`. `BuildThreatUseCase` depends on it via constructor
injection but domain ships no implementation. Whichever module eventually
implements it — most plausibly wherever Sprint 002.75's approved copy
actually lives — is responsible for getting that content reviewed against
§4/§7/§20 before it ships, not `domain`.

This mirrors the `RiskScorer` precedent from ADR 0015: a contract
`domain` depends on and calls, without domain committing to (or being
capable of producing) the actual content/logic behind it.

## Consequences
- `BuildThreatUseCase` is fully testable today with a fake provider (see
  `FakeThreatDescriptionProvider`), without blocking on real copy being
  written — the architecture and the content work are decoupled, which is
  the point.
- Until a real `ThreatDescriptionProvider` is implemented and bound in the
  DI graph, no `Threat` can actually be constructed from a real analysis
  result outside of tests — this is intentional incompleteness, not a
  bug, and should not be worked around with a placeholder implementation
  inside `domain` later. If a "reasonable default" implementation is ever
  wanted (e.g. a generic fallback title before real copy exists), it
  belongs in a higher module and must still be reviewed against Sprint
  002.75, not exempted from that process for being "temporary."
- **Sprint 004C is now feature-complete at the foundation level**, per its
  own stated goal: `ScanTarget` (004B) → `AnalyzeScanTargetUseCase` (004C
  Patch 2) → `AnalysisOutcome` → `BuildThreatUseCase` (004C Patch 3) →
  `Threat`, ready for `CompleteScanSessionUseCase` (004A) to persist. Every
  step in that chain is real, tested, pure-Kotlin, and analyzer-agnostic.
  What's still missing before this does anything a user can see: (a) at
  least one real `ThreatAnalyzer` implementation, (b) a
  `ThreatAnalyzerRegistry` implementation (likely Hilt `@IntoSet`
  multibindings), (c) a real `ThreatDescriptionProvider`, and (d) the
  higher-level orchestration UseCase that runs this whole chain across
  every target in a `ScanRequest` and calls `CompleteScanSessionUseCase`
  with the aggregate results. None of those are "Sprint 004C leftovers" —
  they're genuinely later sprints' work: (a) and (b) need a real detection
  strategy decided first; (c) needs Sprint 002.75's content process; (d)
  is real, non-trivial coordination logic that deserves its own sprint's
  focused attention, not a rushed final patch here.
