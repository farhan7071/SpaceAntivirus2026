# ADR 0034: Scan Results / History screen — and a navigation gap it exposed

**Status:** Accepted

## Context
Sprint 021's brief asked to "replace any remaining placeholder scan summary UI with a dedicated production results presentation." Security Center (Sprint 019) already reactively shows the *latest* scan's threats in detail. What's genuinely still a Sprint 003 placeholder — literally rendering the text "History — placeholder (Sprint 004+)" — is `feature:history`, which was never targeted by name in any prior sprint. That's the real target here: a dedicated view over *every* completed scan, with metadata (time, duration, items scanned) that neither Home nor Security Center currently surfaces in one place.

Before building it, a second real gap was found: `feature:history` was not reachable anywhere in the actual app. It's deliberately excluded from the 4-tab bottom navigation (`TopLevelDestination`'s own KDoc: "Deliberately only 4 destinations"), and nothing else in the app linked to it. Shipping a real History screen with no way to reach it would have been production UI nobody could ever open.

## Decisions

### 1. `HistoryViewModel` reuses `ObserveScanHistoryUseCase` unmapped
Same UseCase `HomeViewModel` and `SecurityCenterViewModel` already use, but where those two take only `.firstOrNull()`, History maps the *entire* list — every completed scan, in the same most-recent-first order the underlying query already provides (Sprint 010). No new UseCase, no new repository method.

### 2. Entry point added on Security Center, not a 5th bottom-nav tab
A "View full history" text button, reached via a new `onViewHistoryClick` callback threaded through `SecurityCenterRoute` — the same callback-based navigation pattern already established for onboarding completion (Sprint 018), not a new pattern. Security Center is the natural place for it: a user already looking at their latest results is the most likely person to want to see past ones.

### 3. `ThreatSummary` duplicated a second time, not yet shared
Same shape as `SecurityCenterViewModel`'s `ThreatSummary`, duplicated locally in `feature:history` rather than extracted to a shared module — this is the second occurrence, and ADR 0032 set the bar at three (rule of three) before that tradeoff is worth making.

### 4. Per-entry detail shown inline, no separate detail screen
Each `ScanHistoryEntry` card shows its own metadata (date, duration, items scanned, clean/not-clean) and, for scans with findings, every threat's title, description, and severity chip directly inside the same card — no navigation to a second "scan detail" screen. Given History already lists every scan with full findings visible, a separate detail screen would be an extra tap for no new information.

## Consequences
- All three of Home's summary, Security Center's latest-scan detail, and History's full list now derive from the exact same `ObserveScanHistoryUseCase` Flow — a scan completing anywhere in the app is reflected consistently and immediately across all three screens with zero additional wiring, a direct benefit of the reactive-Flow architecture established since Sprint 017.
- `SpaceAntivirusNavHost`'s class-level KDoc was updated to reflect which five routes are now real (Onboarding, Home, Security Center, History, plus Home's scan action) versus which remain placeholders — corrected while already touching the file, same discipline as Sprint 018/020's stale-comment fixes.
- History remains reachable only via Security Center for now. If a future sprint finds users need it from elsewhere (e.g., a notification tap-through once notifications are real), that's a small, additive callback wiring change, not an architectural one.
