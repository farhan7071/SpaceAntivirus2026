# ADR 0022: Trusted items are wired into the scan pipeline, with fail-safe trust-check semantics

**Status:** Accepted

## Context
ADR 0021 (Sprint 008) deliberately left `TrustedItemRepository` unwired from `RunScanRequestUseCase`, naming two real open questions: how a skipped-because-trusted target should be counted in `ScanStatistics`, and how `ScanResult.isClean` should treat it. This sprint answers both and does the actual wiring.

A second question came up while writing the wiring: what should happen if the trust-status check itself fails (e.g. the trusted-item store is temporarily unavailable)? Silently treating an unverifiable target as trusted would mean a storage hiccup could cause a real threat to go unscanned — the wrong failure mode for a security product.

## Decisions

### 1. `ScanStatistics.itemsTrusted: Int` — a third breaking change to this model
Same category as ADR 0015 (`Detection.analyzerId`) and ADR 0017 (`itemsInconclusive`). `itemsScanned` now counts only targets actually analyzed; a trusted target is excluded from it and counted separately in `itemsTrusted`. All 6 existing construction call sites were updated in this same patch, not left broken.

### 2. `ScanResult.isClean` does NOT check `itemsTrusted`
Unlike `itemsInconclusive` (which *must* make `isClean` false — an inconclusive result is a coverage gap the analysis failed to close), a trusted item was deliberately excluded by the user's own choice. It's consent, not a gap. Making an otherwise-clean scan report `isClean = false` just because the user trusted an unrelated file would misrepresent what trusting something means, and would actively discourage using the Trusted List feature (using it would make your scans look "less clean"). `TrustedItem`'s own KDoc (Sprint 008) already established this distinction for the model; this decision makes `ScanResult` consistent with it.

### 3. Fail-safe trust-check semantics — the mirror image of ADR 0018's fail-open progress publishing
`RunScanRequestUseCase.isTrustedBestEffort()` treats any failure from `IsTrustedUseCase` as `false` (not trusted) — proceeding with full analysis rather than skipping. This is structurally similar to ADR 0018 (a failure doesn't abort the scan) but semantically opposite in an important way: ADR 0018's progress-publishing failure is tolerated because *losing the update is harmless*. Here, the two possible fail-safe choices are not equally harmless — treating an unverifiable target as trusted (skip it) could hide a real threat; treating it as not-trusted (scan it) costs nothing beyond redundant analysis of an item that probably was actually trusted. The failure is tolerated by picking the *safer* outcome, not by ignoring the failure's consequence.

## Consequences
- `RunScanRequestUseCase` gained a new constructor dependency (`IsTrustedUseCase`) — every call site constructing it directly (test helpers) needed updating; the DI graph itself needs no manual changes since Hilt resolves it automatically once `TrustedItemRepository` has a real binding.
- A target that's both trusted AND would have been flagged by an analyzer never gets flagged — verified directly by a test using an analyzer that *would* flag the target if it ran, so the test fails loudly if the skip logic breaks rather than passing vacuously.
- The remaining piece from ADR 0021's list — whether/how to expose `itemsTrusted` in any future results UI — is still open, but the domain-side question ADR 0021 raised is now fully answered.
