# ADR 0047: Professional Threat Report & Detection Quality Improvements

**Status:** Accepted

## Context
This sprint's brief asked for five things without changing scan architecture, repository structure, or the frozen uninstall feature: reduced false positives for a set of named legitimate apps, a professional threat report format, a four-tier confidence system, an improved scan summary, and UI polish. This ADR documents all five together, since they share one underlying design principle — everything is derived at display time from data this project already has, with no new persistence anywhere in the sprint.

## Part 1 — Detection quality

Applying the same diagnostic discipline as Sprint 031/032: each named app was reasoned about from its likely, real permission profile before any code was written.

- **ChatGPT** and other AI-assistant apps most plausibly declare `PRODUCTIVITY` rather than `VIDEO`/`SOCIAL`/`IMAGE` — `SurveillanceCombinationAnalyzer`'s confidence-modulation set gained `PRODUCTIVITY` alongside the existing `IMAGE`. Deliberately a downgrade, not a suppression like the `VIDEO`/`SOCIAL` case (ADR 0042) — not every productivity app needs a camera and microphone together, so this is real but not unambiguous evidence.
- **TikTok** and other short-form video apps commonly need SMS for account verification but may declare `VIDEO` rather than `SOCIAL` — `SuspiciousPermissionPatternAnalyzer`'s SMS rule gained `VIDEO` alongside `SOCIAL`.
- **Binance** — confirmed this is the same "no Android finance category exists" limitation already documented in ADR 0045/0046. No new fix; the existing limitation still applies and is still the honest answer.
- **Xiaomi Home / Mi Store** — a genuinely new finding: `ConfidenceModulation.TRUSTED_INSTALLERS` only recognized Google Play and Samsung Galaxy Store. Apps installed via Xiaomi's own GetApps store (`com.xiaomi.mipicks`) had neither installer trust nor a matching category available, leaving them at full confidence. Added with the same moderate-not-full confidence disclosure ADR 0045 used for the Samsung package name — not verified against a live device in this sandbox, isolated to one constant if wrong.
- **InDrive** — the same ride-sharing case already handled in Sprint 031. No further scoring change; this sprint's Part 2 report-format work (see below) is the more relevant fix for how this kind of low-confidence finding is communicated.

`CumulativeRiskScorer` remains unchanged again this sprint, confirmed via exact diff scope — every fix stays at the per-analyzer confidence-modulation layer established in ADR 0045.

Not attempted: the brief's own report-format example ("Dynamic Code Loading — reflection usage, runtime dex loading detected") and "distinguishing accessibility usage from abuse" both describe detection capabilities this project doesn't have — APK bytecode analysis and service-level manifest inspection respectively, neither achievable by "improving the scoring model." Treated as a genuine scope boundary, not silently skipped.

## Part 2 & 3 — Professional threat report and four-tier confidence

`ThreatDescriptionProvider` gained two methods:

- `categoryFor(threatType): String` — a short, user-facing label for the report's "Threat Category" field, a pure mapping of data `Threat` already carries.
- `confidenceLevelFor(riskLevel, detections): String` — four tiers (Very High / High / Medium / Low), replacing the three-tier `Confidence` enum value as what's shown to users.

The underlying `Confidence` enum stays exactly three tiers internally — extending it to four was considered and rejected: it would touch every analyzer, `ConfidenceModulation`, and Room persistence for what the brief itself frames as a presentation concern. Instead, "Very High" specifically means `CumulativeRiskScorer` already escalated a `Threat` from two or more independent, meaningful signals agreeing (ADR 0041) — a stronger, more specific statement than any single detection's own confidence could make. This is genuinely "confidence derived from combined analyzer output," not just a relabeling.

`ThreatSummary` (both `SecurityCenterViewModel` and `HistoryViewModel`) gained `threatCategory`; `confidenceLabel` now comes from `confidenceLevelFor` instead of the old `detections.maxOf { it.confidence }` inline computation. `ThreatSummaryCard` displays Threat Category at the top of its expanded state, and its evidence section was restructured into a visually distinct, grouped block with a divider separating it from the recommendation below — directly Part 5's "evidence grouped cleanly" and "recommendations visually separated."

## Part 4 — Scan summary

`ScanSummary` (new, `feature:security`-local) carries all seven requested fields, computed entirely from `ScanStatistics` (existing since Sprint 005/009) and the visible/ignored threat split already established by the Sprint 32.1 fix:

- `appsScanned`, `trustedApps`, `scanDurationMillis` — direct from `ScanStatistics`.
- `threatsDetected` — the currently *visible* threat count, consistent with this screen's existing "protection status reflects what's actually shown" philosophy, not the raw, unfiltered `threatsFound`.
- `ignoredThreats` — deliberately a different number from `trustedApps`, matching the brief's own distinct terms: `trustedApps` (`itemsTrusted`) counts apps skipped from analysis because they were *already* trusted before the scan ran; `ignoredThreats` counts threats *this scan actually found* that have since been marked trusted. Conflating them would misrepresent two different events at two different times.
- `highestThreatLabel`, `averageConfidenceLabel` — both qualitative labels, never raw numbers shown to the user. `RiskLevel`'s own KDoc is explicit that "inflated/numeric risk scores invite alarm-fatigue and aren't something this engine can defensibly back up" (Sprint 002.75 §4); `Confidence`'s KDoc states the identical discipline. `averageConfidenceLabel`'s internal computation is genuinely numeric (mapping each visible threat's tier to an ordinal, averaging, rounding) — but only the resulting tier label is ever returned or displayed. Both fields read "None" when there are no visible threats, rather than defaulting to the lowest tier, which would misleadingly imply a real (if low) confidence about something that wasn't found.

`SecurityCenterUiState.Loaded.scanSummary` is nullable — null when there's no scan yet, genuinely nothing to summarize rather than a zero-valued summary implying a scan happened. Displayed via a new `ScanSummaryCard`, using `AppCard` (Sprint 002.5 §9's single base Card component) rather than a new bespoke card — this is exactly the simple headline/content shape `AppCard` already exists for.

## Part 5 — UI polish

Largely satisfied by Part 2/3's `ThreatSummaryCard` restructuring (evidence grouping, recommendation separation via `HorizontalDivider`) and the existing `SeverityColors`/accent-bar design from Sprint 030 (ADR 0044) — not revisited, since that reasoning was already sound. No new UI polish work was needed beyond what Parts 2–4 already produced as a natural consequence of their own changes.

## Engineering constraints confirmed
- `CumulativeRiskScorer.kt` untouched, confirmed via exact diff scope.
- No Room schema change — confirmed via exact diff scope; `ScanSummary`/`threatCategory`/four-tier `confidenceLabel` are all computed at display time from existing persisted data.
- Uninstall functionality (`requestUninstall`, `openAppInfo`) confirmed present and unmodified via full-file re-read — this sprint's brief explicitly froze it, and nothing in Parts 1–5 needed to touch it.
- No new analyzer added.

## Consequences
- Every test broken by these changes was found and fixed by direct inspection across `core:analysisengine`, `domain`, `core:ui`, `feature:security`, and `feature:history` — 19 files total, all confirmed structurally sound (balanced braces/parens, no line over 120 characters, no duplicate test names) before commit.
- A real mid-implementation correction is worth naming: my first automated attempt to add the new `scanSummary` parameter across `SecurityCenterScreenTest.kt`'s eight construction sites produced inconsistent indentation and only fixed 3 of 8 sites. Reverted cleanly and redone with a bracket-tracking script that correctly handled all eight, including multi-line `threats = listOf(...)` blocks the first attempt missed.
