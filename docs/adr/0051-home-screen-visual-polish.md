# ADR 0051: Home Screen Visual Polish (Sprint 036.5)

**Status:** Accepted

## Context
Refinement only, on top of Sprint 036's already-correct structure (Hero Card → Security Summary → Quick Actions → Recent Activity, unchanged). No layout, ViewModel, repository, or navigation-structure change — confirmed via exact diff scope. No new text content anywhere, which is why `HomeScreenTest.kt` needed zero changes: every existing assertion targets text that's still identical, only its styling, color, and position changed.

## The Hero Card's icon, addressed directly
The brief named a real, specific problem: the status icon "feels visually detached." Investigating confirmed it precisely — the icon appeared twice, once small (16dp) in its own label row, once large (48dp) floating on the far right of the headline row, tied to neither the label above it nor the text beside it. Consolidated into one icon, in a soft tonal circular badge (`ShapeTokens.iconBadge`, new), placed directly beside the status label + headline + supporting text as one visual group, top-aligned so the badge lines up with the label rather than floating at the vertical center of three lines. The Hero Card's elevation was also raised to `Elevation.floating` — it's now visually more raised than the plain cards beneath it, matching "the Hero Card is now the product's visual identity."

## Two genuine semantic mismatches found and fixed while polishing
Not asked for directly, but found while working through Recent Activity's "status icon, typography... should work together" requirement: the activity icon always showed a checkmark — even when threats were found — tinted with the brand primary color for a clean result and flat neutral gray for a dirty one. Backwards: the concerning result read as *less* visually significant than the reassuring one. Fixed to show a real checkmark tinted Safe-green for clean, or a warning icon tinted Attention-amber otherwise.

## Two new SDS tokens, not new hardcoded values
`LayoutTokens.primaryActionHeight` (56dp) — Material3's own default Button height (~40dp) reads as a standard, secondary action, not "the one dominant primary CTA" this sprint asks the Scan Now button to be. `ShapeTokens.iconBadge` — a decorative circular icon container, named separately from `badge`/`chip` (which share its exact value) since it's a different semantic role, matching this file's own established precedent for not collapsing same-valued-but-differently-meant tokens into one name.

## `AppStatCard` gained an optional `accentColor`
Defaults to `null`, preserving every existing call site's exact prior appearance. When provided, tints both icon and value — used only for Threats Found when its value is actually non-zero (a zero-threats card stays neutral), not applied to Trusted Items, which is a neutral count with no inherent positive or negative meaning. "Subtle semantic accents," not increased saturation everywhere.

## Design-lead review pass: one deliberate visual system, not an isolated Hero Card
A second pass through this same sprint, applying "does this look like a generic Material 3 sample or an original, recognizable product" as an explicit review lens rather than only "is this correct." Two further changes came out of it:

**`ShapeTokens.heroCard`** (new, `AppShapes.large` — 16dp) — the Hero Card previously used the same shape token (`ShapeTokens.card`, 12dp) as every other card on the screen. A genuinely distinct, more generous corner radius gives it a different silhouette, not just a different color and elevation — part of what makes a screenshot of this specific card recognizable at a glance.

**The icon-badge motif, extended.** The Hero Card's own tonal circular icon container (built in this sprint's first pass) was, until this point, visually isolated — nothing else on the screen echoed it. Extended the same motif — a tonal circle behind the icon — into `QuickActionCard` and Recent Activity's card, both at `LayoutTokens.minTouchTarget` (48dp), clearly and deliberately smaller than the Hero Card's own 56dp badge, preserving "the Hero Card is clearly more important than every other section." Quick Actions use a neutral, low-opacity brand-primary tint (not status-driven); Recent Activity reuses its own already-computed semantic color (Safe-green or Attention-amber), since that section *is* status-driven the same way the Hero Card is.

**Deliberately left alone:** `AppStatCard`'s own icon treatment. Its layout — a small icon stacked *above* the value, in a compact card — is genuinely different from the icon-beside-text pattern the badge motif fits naturally into elsewhere. Forcing a badge into that tighter, already-compact space would read as cramped rather than systematic; a design decision made and documented, not an oversight.

## Consequences
- 5 files changed: three SDS token additions (additive only — `LayoutTokens.primaryActionHeight`, `ShapeTokens.iconBadge`, `ShapeTokens.heroCard`), `AppStatCard.kt` + its test, `HomeScreen.kt`.
- `HomeScreenTest.kt` needed no changes across either pass — every existing assertion still holds, since no text content changed anywhere in this sprint, only styling, color, shape, and positioning.
- A mistake caught during implementation, not left in: a `str_replace` on `LayoutTokens.kt` accidentally dropped the object's closing brace; caught immediately via a direct brace-balance check before it went any further.
