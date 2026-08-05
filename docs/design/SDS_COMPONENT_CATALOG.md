# SDS Component Catalog — Space Design System v1.0

**Status:** Design specification, Phase 1. Documents the real, currently-implemented
component set in `core:ui` — every entry below reflects verified, existing code,
not an aspirational rewrite. No component was renamed, redesigned, or
behaviorally changed to produce this document.

## A note on naming

This catalog was requested using a `Space*` naming convention (`SpaceCard`,
`SpaceButton`, `SpaceBadge`, `SpaceStatusChip`, `SpaceEmptyState`,
`SpaceSectionHeader`, `SpaceDialog`). This project's actual components use an
`App*` prefix, established starting Sprint 002.5 and consistently followed
across every sprint since — `AppCard`, `AppButton` (three variants),
`StatusChip`, `ScanResultBadge`, `AppEmptyState`, `AppConfirmDialog`.
Renaming eleven components and every one of their call sites is a real,
sweeping code change — out of scope for a documentation-only Phase 1 task
("do not implement or redesign," per this deliverable's own instruction).
This catalog documents the real components under their real names, with
each entry cross-referenced to the `Space*` name it corresponds to, so nothing
requested is missing — only relabeled to match what actually exists.

One requested component, **SpaceSectionHeader**, had no existing
counterpart when this catalog was written (Sprint 035), and was recorded at
the end under **Planned Components** rather than either silently dropped or
built without being asked. Sprint 038 built it, as `AppSectionHeader` — see
**Section Headers** below — once two screens genuinely needed it.

## How to read this catalog

Every component below is documented against the same seven headings this
deliverable requested: Purpose, Anatomy, Variants, Component States, Usage
Guidelines, Accessibility Requirements, Do/Don't Examples. Each entry also
lists the file it lives in and which design tokens (Sprint 035, Phase 1) it
already consumes versus tokens a future Phase 2 pass could adopt.

---

## Cards

### AppCard
*(`Space*` equivalent: SpaceCard)*
**File:** `core/ui/component/AppCard.kt`

**Purpose**
The single base card used everywhere a headline, an optional supporting
line, and arbitrary content need a consistent container — status summaries,
recent activity, and any future screen's simple informational cards.
Established as "one base Card component, content slot varies" specifically
so a future visual change to what a card looks like happens in one place,
not at every screen that built its own.

**Anatomy**
```
┌─────────────────────────────┐
│ Headline                    │  ← required, always present
│ Supporting text              │  ← optional
│                              │
│ [ content slot ]             │  ← arbitrary, caller-provided composable
└─────────────────────────────┘
```

**Variants**
None — deliberately one shape. Content slot flexibility replaces the need
for card sub-variants; see ThreatSummaryCard and ScanSummaryCard below for
the two cases where a genuinely different *structure* (not just different
content) justified a dedicated component instead of using this one.

**Component States**
- **Default** — the only state this component defines. It has no
  interaction of its own (no `onClick`); any interactivity lives entirely
  in whatever is placed in its content slot.

**Usage Guidelines**
- Headline text should be short and specific — a label, not a sentence.
- Supporting text is optional; omit it rather than passing an empty string.
- Do not add a second, competing headline-style element inside the content
  slot — the card's own headline is the one heading readers should look for.

**Accessibility Requirements**
- Headline and supporting text both render as `Text` — already fully
  TalkBack-readable with no extra work from callers.
- If the content slot contains interactive elements, each is responsible
  for its own accessibility (this wrapper adds none of its own touch
  targets to worry about).

**Do / Don't**
- ✅ Do use `AppCard` for a simple status or summary card with a headline
  and optional supporting line.
- ✅ Do reach for a Material3 `Card` composed with `ShapeTokens.card`/
  `Elevation.card` directly, documented and named, if a genuinely different
  structure is needed (as ThreatSummaryCard and ScanSummaryCard both did).
- ❌ Don't build a second, ad hoc "simple card" component elsewhere in the
  app instead of reusing this one — that's exactly the duplication this
  component exists to prevent.
- ❌ Don't put a second headline-weight `Text` inside the content slot.

**Token adoption:** currently uses `LocalSpacing` for internal padding; not
yet updated to `ShapeTokens.card`/`Elevation.card` (it relies on Material3's
own `Card` defaults for shape and elevation). A real Phase 2 candidate.

---

### ThreatSummaryCard
**File:** `core/ui/component/ThreatSummaryCard.kt`

**Purpose**
The card behind every detected app in Security Center and History — the
professional threat report format (ADR 0047). A genuinely different
structure from `AppCard` (a colored severity accent edge, several distinct
always-visible and expandable sections, built-in interactive expand/collapse
state), which is why it's a separate component rather than content poured
into `AppCard`'s slot.

**Anatomy**
```
┌─┬───────────────────────────────────────────┐
│▌│ [icon] App Name              [Severity]  ⋮ │  ← identity row + overflow menu
│▌│      com.example.package                    │
│▌│ [🎥][🎤][🌐]  evidence icon row              │
│▌│ Short one-line summary                       │
│▌│                                               │
│▌│ ▼ View details  (animated expand)             │
│▌│ ┌───────────────────────────────────────┐    │
│▌│ │ Threat Category: ...                    │    │
│▌│ │ Why it was flagged / technical detail   │    │
│▌│ │ ┌ Evidence ──────────────────────────┐  │    │
│▌│ │ │ [icon] Title                         │  │    │
│▌│ │ │        description                   │  │    │
│▌│ │ └───────────────────────────────────┘  │    │
│▌│ │ ┌ Recommendation (icon + title) ──────┐ │    │
│▌│ │ │ text                                │ │    │
│▌│ │ └────────────────────────────────────┘│    │
│▌│ │ Confidence: ...                        │    │
│▌│ └───────────────────────────────────────┘    │
└─┴───────────────────────────────────────────┘
  ↑ colored accent bar (Severity)
```

**Variants**
No named variants — one structure, driven entirely by its parameters
(`severity`, `evidenceIcons`, `evidenceBullets`, etc.). The accent color and
`StatusChip` visually differentiate the three `Severity` tiers without
needing three separate card implementations.

**Component States**
- **Collapsed** (default) — identity, severity, evidence icons, and the
  short summary are visible; the detail section is not rendered.
- **Expanded** — toggled by the "View details" button; animates in via
  `AnimatedVisibility` (`fadeIn() + expandVertically()` / `fadeOut() +
  shrinkVertically()`).
- **Overflow menu open/closed** — the `⋮` button's `DropdownMenu` state,
  independent of expand/collapse.
- No Disabled or Loading state — this card is always fully interactive
  once rendered; it has no async operation of its own (Ignore/Uninstall
  actions are fire-and-forget from the caller's perspective).

**Usage Guidelines**
- Always pass a real `threatCategory` and `confidenceLabel` — both are
  required parameters with no default, since a threat report is
  incomplete without them (a real, previously-shipped bug: two production
  call sites once omitted `threatCategory` entirely).
- `evidenceIcons` (for the always-visible icon row) and `evidenceBullets`
  (for the expanded detail rows) are both derived from the same
  underlying evidence text via `EvidenceIcon.inferFrom` — don't pass
  inconsistent icon/bullet pairs from different sources.
- The three action callbacks (`onIgnoreClick`, `onOpenAppInfoClick`,
  `onUninstallClick`) live behind the overflow menu, not as always-visible
  buttons — this is deliberate, matching the "one primary action, several
  secondary ones" hierarchy this card's own design established.

**Accessibility Requirements**
- Every icon in this card (severity, evidence, menu) uses `contentDescription
  = null` except the overflow trigger itself, which is labeled "More
  actions" — evidence icons are accompanied by their own text title and
  description, so the icon is decorative, not the only carrier of meaning.
- The overflow menu's `IconButton` provides a real touch target via
  Material3's own `IconButton` sizing (already meets the 48dp minimum,
  `LayoutTokens.minTouchTarget`).
- Expand/collapse state is communicated via visible text ("View details" /
  "Hide details"), not icon or color alone.

**Do / Don't**
- ✅ Do let this card own its own expand/collapse state — don't lift it to
  a parent unless a real need (e.g. "collapse all") appears.
- ✅ Do route all three actions through the overflow menu, keeping this
  card's collapsed state visually simple.
- ❌ Don't omit `threatCategory` or `confidenceLabel` — both are required
  for a reason; a caller with genuinely no category should choose an
  honest fallback string, not skip the parameter (there is no default to
  skip to).
- ❌ Don't add a fourth or fifth severity color to this card's accent bar
  — it consumes `Severity`, which is deliberately three tiers only (see
  `Severity`'s own KDoc, `StatusChip.kt`).

**Token adoption:** uses `LocalSpacing`, `ShapeTokens` is not yet wired in
(the card's own `RoundedCornerShape(spacing.small)` predates `ShapeTokens.card`
— identical value, not yet renamed to the token). `Elevation.card` likewise
not yet adopted (uses a literal `2.dp` matching `Elevation.card`'s value).
Both are real Phase 2 candidates, not urgent — the visual output is already
correct, only the *name* consumed differs.

---

### ScanSummaryCard
**File:** `core/ui/component/ScanSummaryCard.kt`

**Purpose**
The dashboard-style scan summary shown above Security Center's threat list
— answers "is my phone safe?" at a glance without scrolling (Sprint 034,
Part 1). Lives in `core:ui`, not a feature module, specifically because it
needs extended Material icons that feature modules don't have available
(a real mistake caught mid-Sprint-034: this card was first built directly
in `feature:security`, which doesn't carry that dependency, before being
moved here).

**Anatomy**
```
┌───────────────────────────────────────────┐
│ [🛡]  All good! / Needs your attention      │
│       Last scan: <time>                     │
│                                              │
│  467          42           421              │
│  Apps scanned Findings     Trusted           │
│                                              │
│  3      4         5           6              │
│  Info   Attention  High Risk   Ignored        │
│                                              │
│  2.3s          Attention        Medium         │
│  Scan duration Highest severity Avg. confidence │
└───────────────────────────────────────────┘
```

**Variants**
One structure; `isProtected: Boolean` switches only the icon
(`IconTokens.security` vs. `IconTokens.warning`) and the headline text
("All good!" vs. "Needs your attention"), not the layout.

**Component States**
- Effectively stateless — every value is a plain parameter the caller
  already computed; this component has no internal `remember`ed state and
  nothing to expand/collapse.
- Not rendered at all when there's no scan yet (the caller's own
  responsibility — `SecurityCenterUiState.Loaded.scanSummary` is nullable
  precisely so this card is simply absent rather than shown with
  placeholder zeros).

**Usage Guidelines**
- Every numeric/text value is pre-formatted by the caller (e.g.
  `scanDurationLabel = "2.3s"`, already formatted, not a raw `Long`) —
  this component does no formatting or unit logic of its own.
- The Info/Attention/High Risk breakdown counts should always sum to
  `findingsCount` — this component doesn't validate that; it's the
  caller's own aggregation to get right (`SecurityCenterScreen.kt`
  currently computes this from `state.threats`).

**Accessibility Requirements**
- Every stat has both an icon (where one exists) and a text label
  underneath its value — never a bare number with no context.
- Icons used purely for stat rows (`IconTokens.security`... etc.) are
  `contentDescription = null`; the adjacent label text carries the
  meaning.

**Do / Don't**
- ✅ Do pass `null` for the parent screen's own `scanSummary` state
  rather than rendering this card with all-zero placeholder values when
  there's no scan yet.
- ✅ Do keep this component in `core:ui`, not a feature module, given its
  extended-icon dependency.
- ❌ Don't invent a fourth breakdown category (e.g. a "Suspicious" count)
  — the three counts shown match `Severity`'s own three real tiers
  exactly; there is no fourth signal to display.

**Token adoption:** uses `LocalSpacing`; not yet updated to consume
`IconTokens` (still references `Icons.Filled.Shield`/`Search`/`CheckCircle`/
`Timer` directly) or `ShapeTokens.card`/`Elevation.card`. A direct, low-risk
Phase 2 adoption target — `IconTokens.security`/`scan`/`trusted` map onto
the exact same icons this component already uses.

---

## Buttons

### AppFilledButton / AppOutlinedButton / AppTextButton
*(`Space*` equivalent: SpaceButton — Primary/Secondary/Text variants)*
**File:** `core/ui/component/AppButton.kt`

**Purpose**
The three button variants this project uses everywhere, wrapping Material3's
own `Button`/`OutlinedButton`/`TextButton` so a future visual change (e.g.
shape morphing on press) happens in one place, not at every feature module
that would otherwise call the raw M3 component directly.

**Anatomy**
Each is a single-slot component: a text label, nothing else. No icon slot,
no loading indicator, no leading/trailing content — see Component States
below for what that means for a not-yet-covered case.

**Variants**
| Variant | Wraps | Typical use |
|---|---|---|
| `AppFilledButton` | M3 `Button` | The one primary action on a screen |
| `AppOutlinedButton` | M3 `OutlinedButton` | A secondary, still-visible action |
| `AppTextButton` | M3 `TextButton` | A low-emphasis action (dialog dismiss, "View full history") |

Two variants named in the original Sprint 034/035 briefs — **Secondary**
(as a filled-tonal style, distinct from Outlined) and **Destructive** (a
red-colored variant for actions like Uninstall) — do not exist as separate
components yet. Documented under **Planned Components** below.

**Component States**
- **Default / Enabled** — `enabled = true` (default).
- **Disabled** — `enabled = false`, forwarded straight to the underlying
  M3 component, which applies its own accessible, theme-aware disabled
  alpha automatically (per `ComponentStateGuidelines.kt`'s own guidance —
  this is the existing, correct pattern that guidance documents, not a
  new behavior).
- **Pressed / Focused** — handled entirely by Compose's own
  `InteractionSource`/ripple on the underlying M3 component; none of
  these three wrappers add or override that behavior.
- **Loading** — not currently supported. `ComponentStateGuidelines.kt`
  specifies what a loading button *should* do (replace its label with a
  same-sized progress indicator, become non-interactive) as guidance for
  a future implementation; none of the three current variants implement
  it yet.

**Usage Guidelines**
- Labels should be short, verb-first (1-3 words) — "Scan Again," not "Tap
  here to start a new scan."
- Use exactly one `AppFilledButton` per screen for the primary action;
  competing filled buttons undermine the "one clear next step" hierarchy
  this variant exists to support.

**Accessibility Requirements**
- Text label is always present and always what TalkBack announces — no
  icon-only button exists among these three variants today.
- Touch target size is inherited from Material3's own button defaults,
  which already meet `LayoutTokens.minTouchTarget` (48dp).

**Do / Don't**
- ✅ Do use these three wrappers everywhere a button is needed — never
  call M3's `Button`/`OutlinedButton`/`TextButton` directly from a feature
  module.
- ✅ Do disable a button via its `enabled` parameter rather than hiding it,
  when the action is temporarily unavailable but still conceptually present.
- ❌ Don't build a custom loading-spinner-inside-a-button composable
  ad hoc at a single call site — that's exactly the kind of duplicated,
  ungoverned pattern this catalog exists to prevent; extend `AppButton.kt`
  itself instead, following `ComponentStateGuidelines.kt`'s documented
  approach, in a future Phase 2 pass.

**Token adoption:** none of the three currently reference `ShapeTokens`,
`Elevation`, or `Motion` directly — they inherit M3's own button defaults
for shape/elevation/ripple timing. `ShapeTokens.button` (a full-round
capsule) was defined in Phase 1 specifically anticipating this component
adopting an explicit shape in Phase 2, rather than relying on M3's default.

---

## Badges & Status Indicators

### StatusChip
*(`Space*` equivalent: SpaceStatusChip)*
**File:** `core/ui/component/StatusChip.kt`

**Purpose**
The severity badge shown on every `ThreatSummaryCard` and reused by
`ScanResultBadge` for non-clean scan sessions. Communicates one of exactly
three severity tiers — see `Severity`'s own KDoc for why this project
deliberately never grew this to four or five tiers, even though the
Sprint 034 reference design showed five.

**Anatomy**
```
┌──────────────────┐
│ [icon] Label       │   ← rounded capsule (ShapeTokens.chip)
└──────────────────┘
```

**Variants**
Three, one per `Severity` value:

| `Severity` | Label | Color | Icon |
|---|---|---|---|
| `INFO` | "Informational" | `SeverityColors.Info*` (blue) | `IconTokens.warning`* |
| `ATTENTION` | "Attention" | `SeverityColors.Attention*` (amber) | `IconTokens.warning`* |
| `ACTION_NEEDED` | "High Risk" | `SeverityColors.ActionNeeded*` (red) | `IconTokens.warning`* |

*All three tiers deliberately share one icon (`Icons.Filled.Warning`) —
color and text label, not the icon, carry the distinction between tiers.
`StatusChip.kt` itself still references `Icons.Filled.Warning`/`Icons.Filled.Info`
directly rather than `IconTokens.warning` — a direct Phase 2 adoption target,
since the underlying icon is already identical.

**Component States**
- **Display only** — no interactive states at all. This is a deliberate,
  documented fix (Sprint 030): the component previously used a Material3
  `AssistChip` with an empty `onClick`, which is inherently interactive
  (ripple, press states) regardless of the lambda's content, producing a
  real reported bug ("looks clickable but does nothing"). The current
  component has no `onClick` parameter at all — there is nothing left to
  wire up accidentally.

**Usage Guidelines**
- Never construct a fourth visual severity treatment by combining this
  component's color with different text — the three defined `Severity`
  values are the complete, real vocabulary.
- If a future screen wants this chip to be tappable (e.g. opening a
  severity explanation), that is a deliberate, additive change to a
  component that is currently honest about having no behavior — not a
  quick `onClick` bolted onto the existing one.

**Accessibility Requirements**
- Color is never the only signal — every chip shows an icon and a text
  label together (Sprint 002.5 §11's own requirement, still honored).
- As a non-interactive element, it correctly has no touch target to
  worry about — TalkBack reads it as static content, matching its actual
  behavior.

**Do / Don't**
- ✅ Do reuse `StatusChip` for any future severity-driven badge need
  across the app.
- ✅ Do rely on `Severity`'s built-in icon/label pairing rather than
  passing raw colors or strings around separately.
- ❌ Don't wrap this component in a `clickable` modifier at a call site to
  "add" interactivity — that reproduces the exact bug this component was
  rewritten to fix, just one layer up.

**Token adoption:** uses `LocalSpacing`, `SeverityColors`, and
`ShapeTokens.chip` (adopted directly in Sprint 035, replacing an inline
`RoundedCornerShape(percent = 50)`) — the most fully token-adopted
component in this catalog.

---

### ScanResultBadge
*(`Space*` equivalent: SpaceBadge)*
**File:** `core/ui/component/ScanResultBadge.kt`

**Purpose**
A scan *session's* own result badge (History's per-session cards,
`ScanSummaryCard`'s protected-status treatment) — a genuinely different
question from `StatusChip`'s "how severe is this one finding." Fixes a
real, shipped bug: History previously called `StatusChip(Severity.INFO)`
for clean sessions, since "Informational" happened to be the mildest
existing value available to reuse — never actually meaning "found
nothing." This component gives that state its own, correctly-labeled
"Safe" treatment instead.

**Anatomy**
Identical capsule shape to `StatusChip` — either renders its own "Safe"
badge directly, or delegates entirely to `StatusChip` for the non-clean
case (same anatomy as `StatusChip` above in that branch).

**Variants**
Two, driven by `isClean: Boolean`:
- **Clean** (`isClean = true`) — "Safe," green (`SeverityColors.Safe*`),
  `Icons.Filled.CheckCircle`. Ignores `highestSeverity` entirely in this
  branch.
- **Has findings** (`isClean = false`) — delegates straight to
  `StatusChip(severity = highestSeverity)`; whatever three-tier value the
  caller passes is shown exactly as `StatusChip` would show it alone.

A fifth badge named in the Sprint 034 reference design — **Suspicious** —
is deliberately not implemented as a distinct badge here, for the same
reason `Severity` itself stays at three tiers: no analyzer or scorer in
this project computes a signal distinct enough from `ACTION_NEEDED` to
justify a separate visual category.

**Component States**
Same as `StatusChip` — display only, no interactive states, for the same
reason (Sprint 030's fixed-`AssistChip` bug).

**Usage Guidelines**
- Always pass the real highest `Severity` among a session's own findings,
  computed by the caller (`HistoryScreen.kt` does this via
  `entry.threats.maxByOrNull { it.riskLevel.ordinal }`) — never a
  placeholder or default value.
- Prefer this component over `StatusChip` directly whenever the subject
  is a whole scan session, not one individual finding.

**Accessibility Requirements**
Identical to `StatusChip` — icon + text label together, no interactive
state, no touch target concerns.

**Do / Don't**
- ✅ Do use `ScanResultBadge` for any session/scan-level result summary.
- ✅ Do compute the real highest severity from actual data before calling
  this component — never hardcode a `Severity` value as a stand-in.
- ❌ Don't reintroduce `StatusChip(Severity.INFO)` as a substitute for
  "clean" anywhere in the app — that's the exact bug this component
  exists to have fixed once, not to reappear at a new call site.

**Token adoption:** uses `LocalSpacing`, `SeverityColors.Safe*`, and
`ShapeTokens.badge` (adopted in Sprint 035, same as `StatusChip`'s
`ShapeTokens.chip` adoption).

---

## Icons

### AppIcon
**File:** `core/ui/component/AppIcon.kt`

**Purpose**
Loads a real app icon via `PackageManager` at display time, keyed by
package name — used everywhere `ThreatSummaryCard` needs to show which
app a finding is about.

**Anatomy**
A single circular image, or — if the app can no longer be resolved (e.g.
uninstalled since the scan that flagged it) — a colored circle showing
the app's own first initial as a graceful, still-informative fallback.

**Variants**
None; `sizeDp` is the only configurable dimension (default 40dp).

**Component States**
- **Loaded** — the real icon, rendered via `AndroidView` wrapping a plain
  `ImageView` (Compose's own official interop mechanism, chosen
  deliberately over manual `Drawable`-to-`Bitmap` conversion to avoid an
  unnecessary `core-ktx` dependency and hand-written `Canvas` code).
- **Fallback** — a colored circle with the app's first letter, shown when
  `PackageManager.getApplicationIcon` throws `NameNotFoundException`
  (expected and common — History routinely shows scans for apps no
  longer installed).

**Usage Guidelines**
- Always provide a real `appLabel`, even when you expect the icon to
  load successfully — the fallback needs it, and there's no way to know
  in advance which path a given package name will take.

**Accessibility Requirements**
- Purely decorative next to the app name/package text that always
  accompanies it in `ThreatSummaryCard` — no `contentDescription` is set,
  which is correct given the adjacent text already names the app.

**Do / Don't**
- ✅ Do rely on the built-in fallback rather than checking package
  existence yourself before calling this component.
- ❌ Don't persist or cache the loaded icon yourself — this component
  re-resolves it fresh via `produceState` keyed on `packageName`,
  matching the "derive at display time, don't persist what's derivable"
  principle this project has applied consistently elsewhere.

**Token adoption:** not yet wired to `LayoutTokens` (its own `sizeDp`
default, 40, is a literal, not `LayoutTokens.minTouchTarget` or any other
named token) — a minor Phase 2 candidate, though 40dp is a deliberately
*visual* size distinct from the *touch target* minimum, so this may not
be the right token to adopt even then.

---

### EvidenceIcon
**File:** `core/ui/component/EvidenceIcon.kt`

**Purpose**
Not a visible component on its own — a small, closed icon vocabulary plus
keyword-inference logic, used by `ThreatSummaryCard`'s evidence icon row
and expanded evidence rows. Included in this catalog because it's exactly
the kind of "reusable, governed vocabulary" a design system exists to
define, even though it renders no UI by itself.

**Anatomy**
Not applicable (data/logic, not a composable).

**Variants**
Six values, each pairing an icon with a short title:

| Value | Icon | Title |
|---|---|---|
| `CAMERA` | `Icons.Filled.PhotoCamera` | "Camera" |
| `MICROPHONE` | `Icons.Filled.Mic` | "Microphone" |
| `INTERNET` | `Icons.Filled.Wifi` | "Internet Access" |
| `SMS` | `Icons.Filled.Sms` | "SMS" |
| `OVERLAY` | `Icons.Filled.Layers` | "Overlay" |
| `OTHER` | `Icons.Filled.Warning` | "Permission" (fallback) |

**Component States**
Not applicable.

**Usage Guidelines**
- `inferFrom(evidenceText: String)` does keyword matching against
  evidence text this project already controls the wording of — it does
  not require or expect any new structured data from the detection
  engine.
- A single evidence bullet can reasonably match more than one icon (e.g.
  a surveillance finding mentions both camera and microphone) —
  `ThreatSummaryCard`'s evidence row deliberately shows only the first
  matched icon per bullet to stay visually simple, not every one that
  matched.

**Accessibility Requirements**
Not applicable directly — accessibility is the responsibility of
whichever component (`ThreatSummaryCard`) actually renders an icon from
this set, which it does with a title and description always present
alongside.

**Do / Don't**
- ✅ Do add a new evidence keyword to an *existing* icon's matching logic
  if a new analyzer's evidence text needs to map to one of these six
  already-defined concepts.
- ❌ Don't add a seventh `EvidenceIcon` value without also verifying the
  new icon name against a real compiler first — every current choice
  beyond `OTHER`'s `Warning` fallback is honestly documented as
  unverified in this sandbox; adding more unverified names compounds
  that risk rather than mitigating it.

**Token adoption:** not applicable — this is itself closer to a token
(an icon vocabulary) than a component; `IconTokens.kt` (Sprint 035) is a
sibling vocabulary for a different, broader set of semantic icons
(Security, Scan, Settings...), not a replacement for this evidence-specific
mapping.

---

## Feedback & States

### AppEmptyState
*(`Space*` equivalent: SpaceEmptyState)*
**File:** `core/ui/component/AppEmptyState.kt`

**Purpose**
The one component every feature's empty state must use, so the principle
"every empty state affirms the positive rather than reading as blank" is
enforced structurally, not left to be silently dropped screen-by-screen.

**Anatomy**
```
┌───────────────────┐
│                     │
│       [icon]         │
│      message          │
│                     │
└───────────────────┘
```
Centered vertically and horizontally, filling available space.

**Variants**
None — `icon` and `message` are the only parameters; every empty state in
the app (no scan yet, no threats found, no history) is this same shape
with different copy and icon.

**Component States**
Static display only — no loading, no interaction.

**Usage Guidelines**
- The `message` should read as reassurance or clear next-step guidance
  ("No threats found. Your last scan didn't detect anything to review."),
  never a bare "Nothing here."
- Reuse this component rather than writing a one-off `Column` with an
  icon and text at a new call site — that duplication is exactly what
  this component exists to prevent.

**Accessibility Requirements**
- Icon uses `contentDescription = null`; the adjacent message text is the
  actual content TalkBack should announce.

**Do / Don't**
- ✅ Do use this for every "nothing to show" state across the app.
- ❌ Don't pass an empty or generic message like "No data" — write real,
  reassuring copy every time.

**Token adoption:** uses `LocalSpacing`; icon choice is left entirely to
the caller (not yet standardized against `IconTokens`) — a real Phase 2
opportunity, since most current call sites already pass
`Icons.Default.Warning`, which `IconTokens.warning` names directly.

---

### AppCircularProgress / AppLinearProgress
**File:** `core/ui/component/AppProgressIndicator.kt`

**Purpose**
Determinate-first progress indicators — progress is shown as a real
percentage wherever the underlying process reports one (a scan, a clean
operation), with indeterminate reserved for genuinely unknown-duration
waits.

**Anatomy**
Each wraps its Material3 counterpart directly (`CircularProgressIndicator`/
`LinearProgressIndicator`) with no additional visual structure.

**Variants**
Two shapes (circular, linear), each with two modes:
- **Determinate** (`progress` is non-null) — shows the real value.
- **Indeterminate** (`progress` is `null`) — the standard M3 animated
  indicator.

**Component States**
The determinate/indeterminate split above *is* this component's state
model — there is no separate Disabled or Error state; a progress
indicator that needs to communicate an error should be replaced by an
error-state component entirely (see **Planned Components** below), not
have this one repurposed.

**Usage Guidelines**
- Prefer determinate whenever the underlying operation can report a real
  fraction — indeterminate should read as a deliberate choice for
  genuinely-unknown-duration work, not a default reached for out of
  convenience.

**Accessibility Requirements**
- Material3's own progress indicators already expose the correct
  accessibility semantics (progress value announced to TalkBack for the
  determinate case) — neither wrapper here adds or removes anything from
  that.

**Do / Don't**
- ✅ Do pass a real `Float` progress value whenever one exists, rather
  than defaulting to indeterminate for simplicity.
- ❌ Don't build a custom progress ring or bar elsewhere in the app —
  these two wrappers are the complete, intended vocabulary.

**Token adoption:** neither currently references `Motion.kt`'s named
durations — indeterminate animation timing is entirely Material3's own
default. Not a strong Phase 2 candidate, since M3's own indeterminate
animation timing is itself already a deliberate, tested design decision
this project has no specific reason to override.

---

## Dialogs

### AppConfirmDialog
*(`Space*` equivalent: SpaceDialog)*
**File:** `core/ui/component/AppDialog.kt`

**Purpose**
The one confirmation-dialog component in the app, reserved for genuinely
blocking confirmations only — never used for informational content (that
belongs in a card or empty state, not a dialog interrupting the user).

**Anatomy**
```
┌─────────────────────────┐
│ Title                     │
│                            │
│ Body text                  │
│                            │
│         [Dismiss] [Confirm] │
└─────────────────────────┘
```
A direct wrapper around Material3's `AlertDialog`, with both actions as
`AppTextButton`s.

**Variants**
None — one shape, parameterized by title/body/two button labels/two
callbacks. There is no non-blocking "toast"-style variant; that's a
deliberately separate, not-yet-built concern (see **Planned Components**).

**Component States**
Binary — shown or not shown, entirely controlled by the caller (this
component has no internal visibility state of its own; a caller
conditionally composes it based on their own state).

**Usage Guidelines**
- Reserve for actions that genuinely need a blocking confirmation before
  proceeding — not as a general-purpose message box.
- Both `confirmLabel` and `dismissLabel` should be short and specific
  ("Uninstall," "Cancel" — not "OK"/"Yes," which don't communicate what
  confirming actually does).

**Accessibility Requirements**
- Material3's `AlertDialog` already provides correct dialog semantics
  (focus trapping, back-button dismissal) — this wrapper changes none of
  that.
- Both buttons are real `AppTextButton`s, inheriting that component's own
  accessibility properties (see **Buttons** above).

**Do / Don't**
- ✅ Do use this for any genuinely blocking yes/no confirmation.
- ❌ Don't use this for purely informational messages the user doesn't
  need to act on — that's a misuse of a component reserved for blocking
  confirmations, and interrupts the user unnecessarily.

**Token adoption:** uses no design tokens directly yet — relies entirely
on Material3's own `AlertDialog` shape/elevation defaults. `ShapeTokens.dialog`
and `Elevation.dialog` were both defined in Phase 1 anticipating this
component's future adoption of them explicitly, rather than only ever
matching M3's defaults by coincidence.

---

## Section Headers

### AppSectionHeader
*(`Space*` equivalent: SpaceSectionHeader)*
**File:** `core/ui/component/AppSectionHeader.kt`

**Purpose**
A consistent section title for screens that group content into several
distinct blocks. Added in Sprint 038, when the bar for extraction was
genuinely met: Home has three section headings (Sprint 036) and the Junk
Cleaner has two (Sprint 038). Before that, Home carried a private
`SectionHeading` composable of its own; it was deleted and switched over
to this component in the same sprint, so there is exactly one
implementation, not two.

That timing is deliberate and worth recording. Sprint 037 round 2 deleted
an earlier `core:ui` extraction (`AppStatGroup`) precisely because it had
one caller and was justified by hypothetical future reuse. The rule that
reversal set — extract when 2+ screens need it *today* — is why this
component exists now and did not exist in Sprint 035.

**Anatomy**
```
┌─────────────────────────────────────────┐
│ Title                        [Action]   │  ← action optional
└─────────────────────────────────────────┘
```

**Variants**
- **Title only** (default) — every current call site.
- **Title + trailing action** — `actionText` + `onActionClick`, rendered
  as an `AppTextButton`. Both must be supplied; a half-specified action
  renders nothing rather than an inert control.

**Component States**
No states of its own. The optional trailing action inherits
`AppTextButton`'s enabled/pressed/focused behavior unchanged.

**Usage Guidelines**
- Place inside a `Column` that already sets its own vertical rhythm
  (`Arrangement.spacedBy`). This component deliberately carries no
  vertical padding of its own, so two spacing systems never fight over
  the same gap.
- Use for section titles within a screen — not as a screen title, which
  belongs to the app bar.

**Accessibility Requirements**
The title is plain text and is read as such. The trailing action is an
`AppTextButton`, which already meets the 48dp touch-target and ripple
requirements — no bespoke clickable `Text`.

**Tokens consumed**
`Type.kt` (`titleMedium`). No colour, shape, or elevation token: the
header sits directly on the screen background.

**Do / Don't**
- **Do** reach for this instead of writing a styled `Text` per screen.
- **Don't** pass `actionText` without `onActionClick` expecting a
  non-interactive label — nothing will render.

## Settings Rows

### SettingsRow
**File:** `core/ui/component/SettingsRow.kt`

**Purpose**
The single row anatomy every Settings screen is built from. Added in
Sprint 043A, when Settings went from three cards to seven sections across
five screens — one component means one set of spacing, one touch-target
guarantee and one accessibility story instead of five that drift.

**Anatomy**
```
┌──────────────────────────────────────────────────┐
│ [icon]  Title                        [control]   │
│         Supporting text                          │
└──────────────────────────────────────────────────┘
```

**Variants** — the trailing control is a closed set, not a slot:
- **None** — informational row.
- **Navigate** — chevron; opens another screen.
- **Toggle** — a Switch. The row is the click target.
- **Selection** — a RadioButton within a single-choice list. The button
  itself is non-interactive; the row handles the click.
- **Value** — read-only current state of a setting owned by another
  screen ("Scheduled Scan — Weekly").
- **Action** — a distinct trailing button ("Remove" on an ignore-list
  entry).

**Why a closed enum rather than a `@Composable` trailing slot**
A slot would be more flexible, and that is the objection. This project's
standing discipline is that a control exists only when it changes real
behavior — Sprint 043A cut three proposed toggles for backing nothing.
Making "add a switch here" a deliberate act rather than a one-line
convenience is the point.

**Component States**
`enabled = false` dims content to 38% and removes the row's click action.
Used for rows whose setting is meaningful but currently inapplicable —
Scheduled Scan while background protection is off, which still persists
the choice for when it is switched on.

**Accessibility Requirements**
The row is the touch target, never the control inside it. A bare `Switch`
is roughly a 32dp target, and TalkBack would announce "switch, on" with
no indication of what it switches. The row therefore carries the role
(`Switch` / `RadioButton` / `Button`), a merged label built from title
plus supporting text, and the single click action; inner controls are
cleared from the semantics tree so they are not announced twice.

`Action` is the deliberate exception — it is a genuinely separate target
with its own meaning, so the row does not merge its semantics and the
button stays independently focusable.

**Tokens consumed**
`ShapeTokens.card`, `ShapeTokens.iconBadge`, `Elevation.card`,
`LayoutTokens.minTouchTarget`, `Spacing`, `MaterialTheme.colorScheme`,
`Type.kt` (`titleSmall` / `bodySmall`). No hardcoded colours; the three
icon sizes are local constants, same convention as Home.

**Do / Don't**
- **Do** reach for this instead of composing a bespoke settings card.
- **Don't** add a variant to `SettingsRowControl` to make a screen look
  fuller. A control with nothing behind it is the thing this catalog's
  own project rules exist to prevent.

## Planned Components

Components requested or implied by the reference design that have no
current implementation. Specified here so they're not silently missing
from the catalog, but explicitly **not built** as part of this
documentation-only deliverable.

### SpaceSectionHeader
**Status:** ✅ Implemented in Sprint 038 as `AppSectionHeader` — see
**Section Headers** above. This entry is kept for the record: it was the
one component in this catalog with no existing counterpart when the
catalog was written.

### Secondary and Destructive button variants
**Status:** Not implemented — see **Buttons** above for full detail.

### A non-blocking, informational toast/snackbar component
**Status:** Not implemented. `AppConfirmDialog` is explicitly reserved
for blocking confirmations; nothing currently fills the non-blocking,
informational-message role a `Snackbar`-based component would.

### An Error-state component
**Status:** Not implemented. `ComponentStateGuidelines.kt` (Sprint 035)
documents how an Error *state* should look when a future component
implements one (reuse the app's existing severity color vocabulary), but
no dedicated "error card" or "error empty state" component exists yet —
today, error states are handled ad hoc per screen (e.g.
`SecurityCenterUiState.Error`, rendered via `AppEmptyState` with a
generic warning icon).
