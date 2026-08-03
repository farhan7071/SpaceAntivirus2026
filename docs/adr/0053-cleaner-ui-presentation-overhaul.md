# ADR 0053: Junk Cleaner UI — Presentation-Layer Overhaul (Sprint 038)

**Status:** Accepted

## Context

Sprint 038 arrived as a six-screen implementation brief with six approved
reference images — Idle, Scanning, Results, Nothing Found, Cleaning
Progress, Cleaning Complete — and an explicit premise:

> Preserve the existing architecture… Business logic already exists.
> Only connect the UI to existing state.

That premise did not hold, and verifying it before writing any code is
the single most consequential thing this sprint did.

## The verification pass, and what it found

Checked directly against `origin/main` at `440ba3b`, not against chat
history or assumption:

**Nothing in this project deletes a file.** Three separate files say so
in their own KDoc, written by the sprints that built them:
`CleanableItem` ("models a CANDIDATE for cleanup, not an action taken —
nothing in this project yet deletes a file"), `FindCleanableItemsUseCase`
("identifies candidates only"), and `CleanViewModel` ("no delete-capable
UseCase or repository method exists yet anywhere in this project…
building a delete button that doesn't actually delete anything would be
exactly the kind of fake production code this project's standing rules
prohibit"). ADR 0035 scoped the junk-file domain layer to candidates
deliberately. A repository-wide search confirms it: there is no delete
use case, no delete repository method, no cleaning engine.

**There is no scan progress.** `FindCleanableItemsUseCase` is a one-shot
`suspend` function returning `AppResult<List<CleanableItem>>` from
`EnumerationRepository.enumerateFiles`. It emits nothing while running.
`CleanUiState.Loading` is a `data object` with no fields. There is no
Flow to observe, no percentage, no per-file callback, and no
cancellation entry point.

**There are no storage statistics.** No `StatFs`, no `StorageManager`,
no total/used/free capability anywhere in the tree.

**There is no cleanup history.** Nothing persists "last cleanup" or
"space freed."

**Two of the four categories in the reference images don't match the
domain.** `CleanableCategory` has exactly four values: `CACHE_FILE`,
`TEMPORARY_FILE`, `LOG_FILE`, `LEFTOVER_INSTALLER`. The references show
"Empty Folders" (which does not exist — `JunkFileClassifier` classifies
files, never directories) and omit leftover installers entirely (which
does exist, and is arguably the most valuable category: stale `.apk`
files in Downloads).

Mapping the six requested screens onto that reality: Screens 1, 3 and 4
are largely buildable; Screen 2 has almost no real data behind it; and
Screens 5 and 6 have none at all.

## Decision

**Stop and report, rather than adapt silently.**

Sprint 037's precedent (Fix #6, scan phases) established that a
presentation-layer *adaptation* — showing honest milestones where the
brief asked for invented phase names — doesn't require stopping for
approval. This is a different situation and a bigger one. Two entire
screens out of six could not be built at all without either fabricating
a cleaning process or building a cleaning domain layer, and the latter
is explicitly outside a presentation-only sprint. Silently shipping four
of six screens, or silently expanding scope into the domain layer, would
both be worse than raising it.

The audit was presented before implementation. The project owner
rescoped:

- **Sprint 038** (this one) — presentation-layer overhaul of the
  existing junk *scanner*: Idle, Scanning (indeterminate), Results, and
  Nothing Found.
- **Sprint 039** — the real cleaning domain layer: delete use case,
  progress Flow, cancellation, storage statistics, cleanup history.
- **Sprint 040** — Cleaning Progress and Cleaning Complete, built on
  Sprint 039's real capabilities.

## What was omitted, and why

Each of these is a deliberate omission under this sprint's own data
rule ("if a value does not exist in the current ViewModel: hide the
element… do not invent fake timers, fake storage values, fake
categories, or fake progress"), not an oversight:

| Reference element | Why it is not on screen |
|---|---|
| Scan percentage (62%), file counters, current file path, countdown | The scan reports nothing while running. A determinate ring would be an animation invented in the UI layer and presented as measurement. |
| `Cancel Scan` | No cancellation entry point on `CleanViewModel`. |
| `Clean 482 MB` / `Clean Now` | Nothing deletes a file. See below. |
| Storage overview (71%, 64 GB, used/free) | No storage statistics provider exists. |
| `Last cleanup — 312 MB freed` | No cleanup history is persisted. |
| `Next recommended scan: Tomorrow` | No junk-scan scheduler exists. |
| Cleaning Progress / Cleaning Complete screens | Deferred to Sprint 040. |
| "Empty Folders" capability row | The classifier never classifies directories. |
| 3D illustrations | No such assets exist in the repo; the SDS's established tonal icon-badge motif is used at hero scale instead. |
| `Done` button | Clean is a top-level bottom-nav destination — leaving is already one tap away on a persistent bar. |

Three of these are locked in by tests that assert their *absence*
(`resultsState_doesNotOfferACleanAction`,
`scanningState_doesNotOfferCancel`,
`idleState_doesNotAdvertiseCapabilitiesTheClassifierLacks`), so they
can't quietly reappear before the capability behind them is real. Those
tests are meant to be deleted by Sprints 039/040, and say so.

## The two judgment calls worth defending

**1. The Results screen has no primary "clean" action at all.**

The obvious alternatives were a disabled `Clean` button, or a `Clean`
button with a "coming soon" message. Both were rejected. A disabled
primary action still communicates "this app cleans your device, just
not right now," which is a promise the Play Store listing would then be
making on the strength of a button that has never worked. Instead the
screen is honest about what it actually is — a report — and says so in
plain words on the hero card: *"Nothing has been deleted — this is a
report of what the scan found."* The primary action is `Scan Again`.

This costs the screen some visual punch. That is the correct trade.

**2. The junk hero is brand teal, not alarm red.**

The reference tints "Junk found" in the same red this app reserves for
`ACTION_NEEDED` security findings. Two established principles say no.
`CleanableCategory`'s own KDoc is explicit that a cache file is not a
security concern and that conflating reclaimable storage with a threat
"would misrepresent what a Cleanable finding actually means to a user" —
and ADR 0015's "never exaggerate risk" discipline applies to the
Cleaner exactly as it does to the detection engine. Colouring 480 MB of
cache the same red as a genuine `ACTION_NEEDED` threat is the textbook
version of the scare-tactic pattern this project has deliberately
avoided since Sprint 002.5. The hero uses the brand's own primary tonal
wash instead. Brand colour also stays deep teal, per the long-standing
decision (Sprint 002.5 §2) reaffirmed against reference images before.

## A design-system finding, and a genuine extraction

**`AppProgressIndicator.kt`'s own KDoc already answered the
indeterminate question.** It states the SDS rule as "determinate-first…
progress is shown as a real percentage wherever the underlying process
reports one. Indeterminate is reserved for genuinely unknown-duration
waits." The junk scan reports nothing, so indeterminate is what that
rule prescribes — not a compromise against the design system, but the
design system's own answer. Checked before assuming, per the habit
Sprint 037 round 2 established after `displayLarge` had gone unnoticed
in `Type.kt` for two rounds.

**`AppSectionHeader` (new, `core:ui`) — and why building it now is not a
repeat of the `AppStatGroup` mistake.** Sprint 037 round 2 deleted
`AppStatGroup` because it had exactly one caller and was justified by
hypothetical future reuse. The bar that reversal set is "2+ screens
genuinely need it today," and it is now met: Home has three section
headings (Sprint 036) and the Cleaner has two (this sprint). This also
closes the one gap `docs/design/SDS_COMPONENT_CATALOG.md` recorded under
**Planned Components** as `SpaceSectionHeader` — built under the `App*`
prefix this project actually uses, exactly as that catalog's own naming
note says every entry should be read. Home's private `SectionHeading`
was deleted and switched over in the same sprint, so this is a genuine
consolidation of two call sites, not a third parallel implementation.

**Typography.** The results total uses `displayMedium`, not
`displayLarge`. `Type.kt` reserves `displayLarge` for exactly two hero
moments (Home's status headline, the scan-complete moment) and says it
"is not used generically." A junk total is neither, so it takes the next
step down rather than quietly widening that reservation to a third case.

**Icon tokens.** Four category icons plus expand/collapse were added to
`IconTokens.kt` in `core:designsystem`, not to `feature:clean`. Feature
modules deliberately do not depend on `compose-material-icons-extended`
(ADR 0031) — a feature that needs a non-baseline icon reaches for a
token, it does not add the dependency to itself. Verified
`feature/clean/build.gradle.kts` before writing any icon reference; this
exact assumption has caused real, caught mistakes in earlier sprints.

**Compose previews — a new precedent.** This sprint adds the first
`@Preview` functions in the project (`CleanScreenPreviews.kt`, four
states × light/dark). `compose-ui-tooling-preview` has been on every
feature module's classpath since Sprint 003 without a single preview
ever being written. For a screen whose acceptance criterion is "matches
an approved reference across four distinct states in both themes," being
able to see all four at once is worth more than anywhere this project
has built UI so far. Kept in a separate file, since previews and their
sample data are development tooling, not part of the screen's
composition.

## What was not touched

`CleanViewModel`, `CleanUiState`, `FindCleanableItemsUseCase`,
`JunkFileClassifier`, `EnumerationRepository`, `CleanableItem`,
`CleanableCategory`, the navigation graph, and every other feature
module except the two-line `SectionHeading` → `AppSectionHeader` switch
in `HomeScreen.kt`. `CleanViewModel` needed no change at all: its four
states map exactly onto the four screens, with `Loaded(items = [])`
already distinguishing "nothing found" from "found something."

## Consequences

- The Cleaner is presentable for a Play Store release *as a junk
  scanner*. It does not claim to be more than that anywhere on screen.
- Sprint 039 and 040 have a clear, honest brief instead of a UI that
  would have needed unpicking.
- Three tests will need deleting as capabilities land. That is intended,
  and each one names the sprint that should delete it.
