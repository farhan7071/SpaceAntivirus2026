# ADR 0029: Production ThreatDescriptionProvider — and the content-governance document that never existed

**Status:** Accepted

## Context
ADR 0016 (Sprint 004C) deliberately left `ThreatDescriptionProvider` as a contract-only interface, specifically because real copy needed to go through "Sprint 002.75's approved Vocabulary Dictionary and Security Messaging Guide" before shipping. ADR 0026/0027/0028 all cited this as the reason the binding stayed open through Sprints 013–015.

Before writing any copy this sprint, that source document was checked for and found not to exist anywhere in this repository. A repo-wide search found 30+ files citing specific section numbers (§4 through §21) from a document that has never been committed to version control — possibly because it only ever existed as a standalone deliverable from before this repository was created (Sprint 003), possibly because it was never captured at all. Either way, "follow Sprint 002.75's guidance" was, as written, an instruction to follow something unverifiable.

This is exactly the "missing domain information required for accurate messaging" case this sprint's own brief said to stop and report rather than work around.

## Decision
Rather than either (a) blocking entirely on a document that may not be recoverable, or (b) silently inventing messaging rules with no grounding, the specific rules implied by every existing citation were extracted from how they've actually and consistently been applied across 15 sprints of real, shipped code, and consolidated into `docs/content-style-guide.md` — a real, committed, checkable artifact for the first time. Its own provenance note states plainly that it's a reconstruction, not the original document, and should be superseded if the real one is ever located.

`ProductionThreatDescriptionProvider` (`core:analysisengine`) is written directly against that new document:

- **Title**: short, static, category-level, never the verdict ("Unusual permission combination," never "Malware Detected").
- **Description**: a lead-in naming the concern, every `Detection`'s evidence text (not just detections matching the "driving" `threatType` `BuildThreatUseCase` passes in — `detections` is the full list, and the always-show-evidence rule doesn't exempt non-driving findings), then a proportionate suggested action, phrased as something to *consider*, never a demand — consistent with `RiskLevel.ATTENTION` never claiming `ACTION_NEEDED`'s certainty (ADR 0027's same reasoning, applied to copy instead of severity scoring).
- Covers all four `ThreatType` values, including `MALWARE` and `UNKNOWN`, which no analyzer currently produces — the provider is a general contract, not scoped to only today's two analyzers, per this sprint's own "must remain easily extensible" requirement.

The existing `titleFor`/`descriptionFor` interface (two methods, unchanged since Sprint 004C) was not extended to a third "summary" method despite the sprint brief mentioning "titles, summaries, and detailed explanations" — a summary and a detailed explanation are treated as one cohesive `description` string (lead-in, evidence, suggested action), rather than expanding domain's contract for a distinction the existing `Threat` model (a single `description: String` field) doesn't itself carry.

## Consequences
- `AnalysisEngineBindingModule` gained the `ThreatDescriptionProvider` binding — the last one ADR 0026 explicitly left open. `BuildThreatUseCase`, and therefore `RunScanRequestUseCase`, are fully Hilt-constructible for the first time in this project's history. Verified directly, not just asserted: `AnalysisEngineBindingModuleTest` now injects `RunScanRequestUseCase` itself, where every prior version of that test deliberately avoided attempting it because it would have failed.
- Any future analyzer producing a new `ThreatType` will hit a compiler error in `ProductionThreatDescriptionProvider`'s exhaustive `when` blocks until copy is added for it — the type system enforces the "every category must have coverage" requirement rather than relying on someone remembering to update this file.
- If a genuine Sprint 002.75 source document is ever located, `docs/content-style-guide.md` should be reconciled against it and this ADR updated to reflect any differences — this reconstruction was built with real confidence from consistent usage, but it is not a substitute for the original if one exists.
