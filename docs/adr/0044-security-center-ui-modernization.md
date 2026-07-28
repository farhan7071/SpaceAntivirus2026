# ADR 0044: Security Center UI/UX Modernization

**Status:** Accepted

## Context
Sprint 029 fixed the report's underlying data problem (app identity, shortened evidence text) but the presentation itself stayed a plain, developer-oriented list. This sprint rebuilds the Security Center — and the History screen showing the same data — into a card-based design inspired by, but not copying, AVG Protection's layout, using an original Material 3 implementation.

## Decisions

### 1. A new, dedicated shared component, not an AppCard variant
`AppCard`'s documented role (Sprint 002.5 §9) is a simple headline/supportingText/content shape — Home's status cards, results summaries. This sprint's card needs a genuinely different structure: a colored severity accent edge, several distinct always-visible and expandable sections, and built-in interactive state (expand/collapse, an overflow menu). Bending `AppCard` to fit that would bloat a component every other screen also depends on with concerns specific to one kind of card. `ThreatSummaryCard` (`core:ui`) is built once, standalone, and shared by both `SecurityCenterScreen` and the History screen — satisfying "both screens should share the same UI components where practical" directly rather than duplicating the layout.

`core:ui` gained real `androidTest` infrastructure for this — it had none before; nothing built there previously was substantial enough to need it.

### 2. Collapsed-by-default, matching the reference layout's own affordance
Collapsed: app icon, name, package, severity chip, evidence-type icons, and one short summary sentence. Tapping "View details" (with a chevron, matching the reference mockup's own `>` affordance) expands to show the technical explanation (`Threat.description`, kept exactly for this since Sprint 029 deliberately preserved it as "legacy, not yet used by primary display" — that decision pays off directly here), the full evidence bullet list, and the recommendation. Expand state is local, unlifted Compose state — pure presentation, nothing to persist or observe.

### 3. Severity chip: fixed, not just re-skinned
The reported bug was real and specific: `StatusChip` was a Material3 `AssistChip` with an empty `onClick` lambda — `AssistChip` is inherently interactive (ripple, press states) regardless of what the lambda does, so it looked clickable while doing nothing. Rewritten with no `onClick` parameter at all, so there is nothing left to wire up incorrectly. This is also the first real use of `SeverityColors` (defined in the design system since early in this project, never actually wired into any component until now) — light/dark variants selected via `isSystemInDarkTheme()`.

### 4. Evidence icons and contextual copy: keyword inference, not new structured fields
`Detection` gained no new field for this. Icons (`EvidenceIcon`, `core:ui`) and contextual recommendations/summaries (`ProductionThreatDescriptionProvider`, `core:analysisengine`) both infer from the same `evidenceDescription` text this project already controls the exact wording of (verified and shortened in Sprint 029). This is a deliberate, small duplication of keyword logic across two layers rather than either giving `core:ui` a new dependency on `domain` (which it has never had) or paying for another Room migration and touching all eight analyzers again for what is fundamentally a presentation concern. `ThreatDescriptionProvider.recommendationFor` was extended to take `detections` and `riskLevel`, not just `threatType` — threat type alone is too coarse (every permission-combination analyzer shares `SUSPICIOUS_PERMISSION_USAGE`), and urgency (`ACTION_NEEDED`) takes precedence over evidence-specific framing when both could apply.

The exact extended-icon names chosen (`PhotoCamera`, `Mic`, `Wifi`, `Sms`, `Layers`) have not been verified against a real compiler in this sandbox; each was picked for being among the most stable, long-standing names in the Material icon set, and every uncertain category falls back to `Icons.Default.Warning`, the one icon this project has verified safe since Sprint 017. If any name is wrong, it is an isolated, one-line fix in `EvidenceIcon.kt` — nothing else in this sprint depends on which exact icon renders.

### 5. App icons load at display time, not persisted
`AppIcon` (`core:ui`) queries `PackageManager.getApplicationIcon(packageName)` when composed, keyed only by the package name already available. Rendered via `AndroidView` wrapping a plain `ImageView` — Compose's own official interop mechanism, chosen deliberately over manual `Drawable`-to-`Bitmap`-to-`Canvas` conversion, which would have needed either a `core-ktx` dependency this module doesn't have or hand-written conversion code, both avoidable risk for a problem `AndroidView` already solves safely. Falls back to the app's first letter on a colored circle if the app has since been uninstalled (a real, expected case in History) or the icon otherwise can't load. Not persisted: an icon is large, mutable, and entirely re-derivable from a field (`targetIdentifier`/package name) that already exists — the same reasoning Sprint 029 already applied to `recommendationFor`.

### 6. Real actions: Ignore, Open App Info, Uninstall
"Ignore" calls `AddTrustedItemUseCase` — a mechanism that has existed since Sprint 008 and was never wired to any UI anywhere in this project until now. "Open App Info" and "Uninstall" are standard, permission-free Android Intents (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`; `Intent.ACTION_DELETE` with a `package:` URI) launched directly from each screen via `LocalContext.current`, deliberately never routed through a ViewModel — a ViewModel should never hold a `Context` reference. Both intents hand off to system UI the user must explicitly confirm; neither performs the action directly, so neither needs a permission this app doesn't already have.

Uninstall is offered unconditionally, with no `isSystemApp` check needed at the UI layer: every one of this project's eight analyzers already excludes system apps before ever producing a `Detection` (ADR 0027 onward), so any app a `ThreatSummaryCard` can even be built for is already guaranteed non-system by construction.

### 7. History's per-session card is kept, only its per-app content changed
`ScanHistoryEntryCard`'s outer `AppCard` (date, apps-scanned/duration summary) represents a genuinely different kind of card — one scan session, not one flagged app — and was never a candidate for replacement by `ThreatSummaryCard`. Only the content inside it, one `ThreatSummaryCard` per flagged app, changed.

## Consequences
- Every test broken by these changes was found and fixed by direct inspection: `SecurityCenterViewModelTest`, `SecurityCenterScreenTest`, `HistoryViewModelTest`, `HistoryScreenTest`, plus new, dedicated `ThreatSummaryCardTest` and expanded `ProductionThreatDescriptionProviderTest` coverage for the new contextual logic.
- `feature:security` and `feature:history` both gained `AddTrustedItemUseCase` and `ThreatDescriptionProvider` as ViewModel dependencies — both were already reachable via the existing feature convention plugin (`core:ui`/`domain` are wired in automatically), so no new module dependency was needed.
- The exact extended-icon names in `EvidenceIcon` remain the one area of this sprint not verified against a real compiler — flagged explicitly rather than silently assumed correct.
