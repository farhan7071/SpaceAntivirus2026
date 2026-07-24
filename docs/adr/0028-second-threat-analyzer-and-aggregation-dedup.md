# ADR 0028: Second ThreatAnalyzer, and real deduplication in the aggregator

**Status:** Accepted

## Context
Sprint 014 proved the plug-in architecture with one analyzer. Sprint 015's brief asked for two things together: a second, genuinely complementary analyzer, and strengthening the aggregation pipeline specifically to avoid duplicate findings when multiple analyzers identify similar concerns.

## Decisions

### 1. `AppIdentityImpersonationAnalyzer` — a different risk dimension, not a variation
Sprint 014's analyzer asks "does this app's *capabilities* look suspicious" (permission combinations). This one asks "is this app *pretending to be something it isn't*" (identity). It flags an app only when its display label is an **exact** match to one of a small, curated list of extremely well-known app names (WhatsApp, Instagram, Facebook, Google Chrome, Google Play Store) AND its package name doesn't match that brand's real, official package. Both conditions are deliberately conservative:
- Exact label match, not substring/fuzzy — an app merely mentioning a brand ("WhatsApp Backup Tool") is not flagged.
- A short, high-confidence list — an incorrect entry would cause a false positive on the *genuine* real app, a far worse failure than under-covering less common brands.

Uses `ThreatType.POTENTIALLY_UNWANTED_APPLICATION`, distinct from Sprint 014's `SUSPICIOUS_PERMISSION_USAGE` — impersonating a brand is a deception concern, not a permission-usage one, and the distinct category is also what keeps the two analyzers' findings naturally non-overlapping rather than looking like duplicate evidence for the same concern.

System apps are excluded entirely, same reasoning as Sprint 014.

### 2. Real deduplication added to `AnalysisOutcomeAggregator`
Concatenated `Detection`s across all `Flagged` outcomes are now deduplicated by exact `(threatType, riskLevel, evidenceDescription)` match before being placed in the final outcome — the first occurrence is kept.

This is carefully scoped to not contradict the aggregator's existing, load-bearing "never drop evidence" rule (documented since Sprint 004C): that rule is about never discarding a `Detection` because a *different* analyzer disagreed or found something else. Deduplication only ever collapses detections that are, in substance, saying the exact same thing — two analyzers independently reaching an identical conclusion isn't two pieces of evidence, it's one piece of evidence confirmed twice, and presenting it as two identical rows would misrepresent its actual weight.

A real regression was found and fixed while implementing this, worth recording precisely: the existing `AnalysisOutcomeAggregatorTest`'s detection-fixture helper generated the identical placeholder `evidenceDescription = "evidence"` for every test detection regardless of id. The new dedup logic would have silently collapsed that test's 3-detection "concatenated, not dropped" scenario down to 1, breaking a load-bearing existing test. Fixed by giving the fixture genuinely distinct evidence text per id, and adding dedicated new tests that deliberately construct identical evidence to test deduplication itself as its own concern.

### 3. Both analyzers stay in `core:analysisengine`
Same reasoning as ADR 0027 — both are small, pure Kotlin, no reason to live anywhere else. Adding the second analyzer required zero changes to `ThreatAnalyzerRegistry`, `AnalyzeScanTargetUseCase`, or `AnalyzerExecutor` — only one new `@Binds`/`@IntoSet` line in the existing binding module. This is the plug-in architecture working exactly as designed since Sprint 004C/006.

## Consequences
- `RunScanRequestUseCase`, run against a device, now genuinely executes two independent production analyzers per application target, aggregates their findings (deduplicated where genuinely identical, both preserved where genuinely different), and remains fault-isolated if either — or a third, hypothetically broken — analyzer fails. `AnalysisPipelineIntegrationTest` verifies this directly with two real analyzers plus a deliberately broken third, not just fakes.
- Sprint 013/014's Hilt smoke test needed its exact-count assertions updated again (1 → 2) — the same kind of update Sprint 014 itself made to Sprint 013's version of this file. This is now an expected, recurring maintenance point every time a new analyzer is registered, not a one-off.
- A second corruption incident of the exact same class as Sprint 006/010 (a `str_replace` silently eating a test's function signature) was caught during this sprint's self-review, this time immediately, by the now-standard practice of reading the full file after every edit rather than trusting the edit succeeded as intended.
