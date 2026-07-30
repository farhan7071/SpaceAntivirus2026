# ADR 0048: Final Security Center UI Polish

**Status:** Accepted

## Context
This sprint was UI/UX only, using a provided design mockup as inspiration rather than a pixel-perfect spec, explicitly not to be copied exactly. Every decision here was made against one constraint: no scanning engine, analyzer, repository, Room schema, or business logic change — confirmed via exact diff scope, every touched file lives in `core:ui`, `core:designsystem`, or the two screens' own UI layer.

## The 5-tier badge question, decided before any code was written
The mockup shows five visual severity levels (Trusted, Informational, Attention, Suspicious, High Risk). `Severity`'s own KDoc, unchanged since Sprint 030, is explicit: "the ONLY three severity tiers this app uses... deliberately not a 5-tier scale" (Sprint 002.5 §17, Sprint 002.75 §4). This sprint kept it at exactly three. Inventing a fourth or fifth tier with nothing real underneath it — no analyzer or scorer in this project computes a "suspicious but not quite high-risk" signal distinct from `ACTION_NEEDED` — would have been presentation dishonesty, directly working against "user trust," one of this same sprint's own stated design goals. "Trusted"/green is real, but represents a genuinely different concept (the absence of any finding) from what `Severity`'s three tiers measure (how severe a specific finding is), so it's handled as a separate visual treatment (`ScanResultBadge`, `SeverityColors.Safe*`), never a fourth enum value.

## A critical, pre-existing bug found and fixed
While wiring `threatCategory` into `HistoryScreen.kt`'s call site, discovered that both `SecurityCenterScreen.kt` and `HistoryScreen.kt` were already missing that required `ThreatSummaryCard` parameter — a genuine compile-breaking bug from Sprint 033 that evidently slipped past that sprint's own verification, since this sandbox has no real compiler to catch it directly. Fixed in both files, confirmed via direct search that every production call site now passes it.

A second, real bug found while building `ScanResultBadge`: `HistoryScreen.kt`'s clean-session case called `StatusChip(Severity.INFO)` — "Informational" was never the right word for "found nothing," it was just the mildest existing severity value available to reuse. Fixed with a dedicated, correctly-labeled "Safe" badge.

## Architectural correction mid-sprint
The dashboard scan summary was initially built directly in `SecurityCenterScreen.kt` using `Icons.Filled.Shield`/`Search`/`CheckCircle`/`Timer`. Verified afterward that `feature:security` has no `compose-material-icons-extended` dependency — unlike `core:ui`, which already carries it specifically for `ThreatSummaryCard`, `AppIcon`, and `EvidenceIcon` (ADR 0031's standing caution deliberately keeps feature modules restricted to `Icons.Default.Warning`). Would have failed to compile. Reverted cleanly and rebuilt `ScanSummaryCard` as a new, shared `core:ui` component instead, taking simple, already-formatted UI parameters — architecturally consistent with every other component this module already hosts for exactly this reason.

## Part-by-part summary

**Part 1 (Scan Summary dashboard):** `ScanSummaryCard` (new, `core:ui`) — large status icon, "All good!"/"Needs your attention" message, last scan time, and stat grid (apps scanned/findings/trusted, then the INFO/ATTENTION/ACTION_NEEDED breakdown, then duration/highest severity/average confidence). The severity breakdown is a pure UI-layer aggregation of `state.threats`, computed in `SecurityCenterScreen.kt` itself — the same kind of derived-at-display-time computation `RiskLevel.toSeverity()` already performs in that same file, not a new ViewModel computation.

**Part 2/4 (Threat Cards, Evidence):** evidence bullets now render as icon + short title + description rows (`EvidenceRow`), using `EvidenceIcon`'s new `title` field — a pure UI-layer addition, no analyzer or evidence text changed.

**Part 3 (Risk Badges):** `Severity` gained per-tier icons and relabeled ("Informational"/"Attention"/"High Risk"), still exactly three tiers — see above.

**Part 5 (Recommendations):** the recommendation section gained a light background surface (`primaryContainer`) and icon, replacing the earlier divider-separated plain text.

**Part 6 (Layout):** the existing structure (`ScanSummaryCard` → threat cards → "View full history" button, consistent `spacedBy` spacing throughout) already matched the suggested scrolling rhythm; no changes were needed here beyond what Parts 1–5 already produced.

**Part 7 (History):** session cards show apps scanned/findings/duration as separate fields instead of one cramped combined string, with the new `ScanResultBadge`. "Suspicious," named in the reference design, is deliberately not a distinct badge — same reasoning as the Severity discussion above.

**Part 8 (Visual Polish):** rounded corners applied to both cards' shapes; expand/collapse now animates via `AnimatedVisibility` (`fadeIn`/`expandVertically`/`fadeOut`/`shrinkVertically`), needing an explicit `compose-animation` dependency added to `core:ui` and the version catalog — not assumed transitively available, given "Gradle Sync PASS" is an explicit verification requirement this sprint asked for.

## Consequences
- 16 files changed: `core:ui` (7 new/changed components plus 4 new dedicated test files), `core:designsystem` (one color addition), 2 feature screens and their tests, and the build configuration for the one new dependency.
- Every test broken by these changes was found and fixed by direct inspection, plus new dedicated tests added for every genuinely new component (`ScanSummaryCard`, `ScanResultBadge`, `StatusChip`) rather than relying only on indirect coverage through the screen tests.
- A real ambiguity caught while writing tests: asserting on the text "Attention" in a scan-summary test would have matched two nodes simultaneously (the dashboard's breakdown label and a threat card's own `StatusChip`), which Compose's `onNodeWithText` rejects by default. Removed that specific assertion, relied on unambiguous text instead, and documented why directly in the test.
