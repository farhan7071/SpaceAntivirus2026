# ADR 0049: Space Design System (SDS) v1.0 — Token Layer

**Status:** Accepted

## Context
Sprint 035's brief asked for a complete design system foundation, referencing a mockup for visual inspiration, explicitly not to be copied exactly. This is Phase 1 only: the design-token layer — colors, typography, spacing, shape, elevation, motion, icons, layout, and component-state guidelines. No screen was touched, no component was rebuilt; that is deliberately scoped to a later phase.

## Architecture
Every token lives in `core:designsystem/theme/`, one file per category:

```
BrandColorTokens.kt       — raw brand palette
SemanticColorTokens.kt    — SeverityColors, SpaceColors (meaning, not brand)
Type.kt                   — AppTypography (full 15-slot M3 scale)
ShapeTokens.kt             — AppShapes (raw scale) + ShapeTokens (semantic: button/card/dialog/bottomSheet/badge/chip/navigation)
Spacing.kt                 — Spacing/LocalSpacing (element-to-element gaps)
LayoutTokens.kt             — screen padding, content max-width, touch targets, list-row heights
Elevation.kt                 — five named tiers (flat/card/floating/dialog/overlay)
Motion.kt                     — named animation durations
IconTokens.kt                  — semantic icon mapping (one icon family, named by role)
ComponentStateGuidelines.kt     — guidance + ComponentState enum for Loading/Error
```

`Color.kt` and `Shape.kt` were split, not edited in place — brand tokens and semantic tokens are conceptually different questions ("what does Space Antivirus look like" vs. "what does orange mean here"), and one file answering both made that distinction easy to lose. Verified nothing referenced either old file by path before removing them (Kotlin resolves imports by symbol, not filename).

## Two brand-identity decisions, made deliberately, not by default

**The brand primary stays teal, not the mockup's "Security Green."** This color was already chosen deliberately, with documented reasoning, early in this project's history: explicitly *not* the red/black "hacker aesthetic" competitors use. Every competitor this sprint names as inspiration (Bitdefender, Norton, Malwarebytes) already uses a green-primary security palette. This sprint's own brief says "create an original identity... do not copy them." Following the mockup's literal color would mean copying the exact convention this project already chose to differentiate from. "Security Green" isn't discarded — it's formalized as its own named semantic token (`SpaceColors.securityGreen`) for protected/trusted states specifically, distinct from the brand primary.

**Dynamic color now defaults to off.** It was previously on by default on Android 12+ — meaning on a majority of the current install base, the app's actual on-screen colors were never the deliberately-chosen brand teal at all, but whatever each user's wallpaper happened to generate. A security app's brand identity should be consistent and recognizable across users; wallpaper-dependent color works directly against that. The parameter itself is untouched — callers can still opt back in explicitly.

## Two color tokens defined without being wired to real severity
`SeverityColors.Safe` (Sprint 034) and `SeverityColors.Suspicious` (this sprint) are both defined, named, real colors — and deliberately *not* added as a fourth/fifth `Severity` enum value. `Severity`'s own long-standing KDoc is explicit: three tiers only, by design, because no analyzer or scorer in this project computes signals distinct enough to justify more. Defining the color token without wiring it to invented data keeps the design system's vocabulary complete for whichever future, real use needs it, without the design system itself inventing detection nuance that doesn't exist. This same reasoning is why `ComponentState` (Loading, Error) intentionally does *not* also define Pressed/Hovered/Focused — Compose's own `InteractionSource` already handles those correctly; naming a parallel token would either duplicate free platform behavior or invite a worse, hand-rolled reimplementation of it.

## Tokens wired into real components, not left abstract
Two pre-existing components — `StatusChip` and `ScanResultBadge` — each hardcoded `RoundedCornerShape(percent = 50)` inline, the same shape decision made twice with no shared name. Both now consume `ShapeTokens.chip`/`ShapeTokens.badge`. This is the one piece of "component work" in an otherwise token-only sprint, done specifically because it's a direct instance of the exact problem this token layer exists to prevent — not scope creep into screen or component redesign.

## Deliverable format
`core:designsystem` gained an explicit `compose-material-icons-extended` dependency, needed for `IconTokens.kt` — verified as genuinely absent before adding it (given this project's own prior, real mistake of assuming a module had this dependency when it didn't). No other module dependency, Room schema, ViewModel, navigation graph, or business-logic file was touched — confirmed via exact diff scope.

## Consequences
- Phase 2 (component implementation — buttons, cards, badges, status components) and any screen adoption both build on this token layer, not before it exists, per the sprint's own stated sequencing.
- `ComponentState` currently has two values and no consumer yet — deliberate: this phase defines the vocabulary Phase 2 implements against, not a redesign of `AppButton`/`AppCard` ahead of schedule.
