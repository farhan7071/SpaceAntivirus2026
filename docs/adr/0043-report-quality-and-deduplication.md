# ADR 0043: Report quality and deduplication — the real root cause

**Status:** Accepted

## Context
Real-device testing of Sprint 028's refined detection engine reported the scan report as harder to read than Sprint 027's, despite better detection logic: duplicate-looking "Unusual permission combination" findings, verbose repetitive explanations, apps seeming to appear multiple times, and no way to quickly identify which app a finding was about.

Before writing any code, the specific claim — that the same app produces multiple separate report entries — was traced through every layer of the pipeline: `AnalysisOutcomeAggregator` (domain aggregation), `RunScanRequestUseCase`'s scan loop, Room persistence (`ThreatDao`/`DetectionDao`, no JOIN fan-out), and `SecurityRepositoryImpl`'s reconstruction. All four already correctly produce exactly one `Threat` per app per scan — this was proven directly with an end-to-end test in Sprint 027 and re-verified here, not assumed.

## The actual root cause
`Threat` never carried the application's display name. `SecurityCenterScreen`'s `ThreatCard` showed `threat.title` — a generic, `threatType`-derived category label like "Unusual permission combination" — as its headline, never the app's actual name. When different apps triggered the same `threatType`, they showed *identical* headline text in the list — visually indistinguishable from literal duplication, even though the underlying data was already correct and already deduplicated. This is what real-device testers experienced as "duplicate findings": genuinely different apps, invisible as such.

## Decisions

### 1. `Threat.appLabel` and `ScanTarget.displayLabel` — the actual fix
A new, defaulted (non-breaking) field on `Threat`, populated by `BuildThreatUseCase` via a new `displayLabel` extension on `ScanTarget` (parallel to the existing `identifier` extension). `BuildThreatUseCase` gained a `target: ScanTarget` parameter for this — `outcome.targetIdentifier` and `target.identifier` are always the same String in practice (the caller always analyzes the exact target it's building a Threat from), so this adds no correctness risk, only the missing display data.

### 2. `SecurityCenterScreen`'s `ThreatCard` restructured: app identity first, evidence as bullets, recommendation separate
`ThreatSummary` changed from `(title, description, riskLevel)` to `(appLabel, packageName, riskLevel, reasons, recommendation)`. `reasons` is simply `detections.map { it.evidenceDescription }` — no new data, just no longer flattened into one paragraph. This directly satisfies goals #1, #4, and #5: application identity shown before any explanation, one bullet per finding instead of one run-on paragraph.

### 3. Evidence text shortened across all 8 analyzers, substrings verified precisely before and after
Every analyzer's `evidenceDescription` was rewritten to 1-2 concise sentences. Every existing test's required substring (`"SMS"`, `"INTERNET"`, `"device administrator"`, `"impersonating"`, `"draw over other apps"`, `"camera"`, `"microphone"`, `"unrecognized source"`, and dynamic package-name/namespace interpolations) was checked against the exact resulting string value — not assumed preserved — before finalizing each change.

### 4. `ThreatDescriptionProvider.recommendationFor(threatType)` — new, deliberately not persisted
A short, actionable, `threatType`-keyed recommendation, distinct from `descriptionFor`'s existing longer prose (which is kept unchanged and still persisted — removing it would be exactly the large refactor this sprint's own constraints ask to avoid, for a field with no caller that actually needs it deleted). Deliberately *not* added as a `Threat` field: it's purely a function of `threatType`, which is already persisted, so recomputing it at display time avoids a Room schema change for data entirely derivable from an existing field.

### 5. A second, real, separate bug found and fixed while extending this exact schema
`Detection.confidence` (added Sprint 027) was never persisted — `DetectionEntity` never got a `confidence` column, so it silently reverted to its default on every read from the database, regardless of what an analyzer actually set. Not yet user-visible (nothing currently displays confidence directly, and the aggregate `RiskLevel` — which *is* persisted — is computed once at scan time, not recomputed from reconstituted Detections on read), but a genuine data-integrity gap, closed alongside the main fix rather than left for a future sprint to rediscover.

### 6. Room schema version 3 → 4, `fallbackToDestructiveMigration()`, no new Migration object
Matches this project's own established, documented policy (ADR 0023) for a pre-1.0 app with no real persisted rows across any prior version bump.

### 7. `feature:history` deliberately left unchanged
It has its own separate, local `ThreatSummary`-equivalent type (never shared between feature modules in this project) and remains fully functional with the old `Threat.title`/`.description` fields, which still exist unchanged. Reworking it identically to `SecurityCenterScreen` is a real, valuable, but separate effort — deferred rather than expanded into this sprint's scope, consistent with "avoid large refactors unless necessary."

## A process note worth recording
During this sprint's implementation, a working tree was built from a clone that predated Sprint 028's actual push to `origin/main`, and Sprint 028's fixes were initially, incorrectly assumed missing. This was caught by the person reviewing the work, not found independently — the correct fix (re-verifying against a genuinely fresh clone, then redoing the affected work with each file's real current state checked before editing, rather than reconstructing anything from memory) is what's reflected in this patch. Recorded here because it's exactly the kind of thing this project's "document architectural problems, don't silently work around them" standing instruction is meant to catch — including problems in how a sprint's own work was carried out, not just problems in the code.

## Consequences
- Every test broken by these changes was found and fixed by direct inspection (`grep` for construction sites, not assumption): `BuildThreatUseCaseTest`, `ThreatBuildingPipelineIntegrationTest`, `ThreatDaoTest`, `DetectionDaoTest`, `FakeThreatDescriptionProvider`, `SecurityCenterViewModelTest`, `SecurityCenterScreenTest`.
- `feature:history`'s own identical restructuring remains a clearly-named, deferred next increment, not a silent gap.
