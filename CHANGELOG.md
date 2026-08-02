# Changelog

All notable changes to this project are documented here. Format loosely
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); this
file starts with Sprint 026's real-device hotfix rather than
retroactively documenting every prior sprint, since ADRs already serve
as this project's detailed historical record (`docs/adr/`).

## Sprint 037 — Home Screen Premium UI Polish

Six numbered, mandatory design corrections against reference images,
followed by a stricter second design-review round. No ViewModel,
repository, scan engine, navigation architecture, or business logic
touched.

### Changed

- **Hero Card**: reduced padding (~20-25% height reduction), Dismiss
  moved to a top-right text action. Headline now uses `displayLarge` —
  the design system's own typography scale already reserved this style
  for exactly this "status headline" moment, but it had never actually
  been applied here until this sprint. The separate icon badge beside
  the headline (added in an earlier polish pass) was removed, since it
  competed with the larger headline rather than reinforcing it.
- **Security Summary**: now one connected dashboard container instead
  of two separate cards.
- **Quick Actions**: smaller icon badges, tighter padding, denser
  cards — touch targets remain fully accessible.
- **Vertical spacing**: reduced between all top-level sections.
- **Unknown State copy**: warmer, onboarding-style language, both on
  the Hero Card and Recent Activity's empty state.
- **Scan progress messages**: replaced the flat "Scanning… N of M"
  counter with milestone-based messages (Starting / Scanning / Almost
  done) derived from real progress data.

### Not changed, deliberately

- Scan progress does **not** show named technical phases ("Analyzing
  permissions…", "Scanning app signatures…") as literally requested —
  `ScanProgress` has no phase data at all, and displaying invented
  phase claims would misrepresent what the scan engine can actually
  verify at any given moment.
- No new `core:ui` component was kept from this sprint. An initial
  extraction (`AppStatGroup`) was removed again once a stricter review
  round caught that nothing else in the codebase actually used it —
  the same connected-dashboard visual is now simple, local code inside
  `HomeScreen.kt` instead.

See ADR 0052 for the full design analysis and reasoning behind each fix.

## Sprint 036.5 — Home Screen Visual Polish

Refinement only, on top of Sprint 036's already-correct structure. No
layout, ViewModel, repository, or navigation change. No text content
changed anywhere, so `HomeScreenTest.kt` needed zero updates.

### Improved

- **Hero Card icon** — previously appeared twice, disconnected from the
  text it represented; consolidated into one icon in a soft tonal
  circular badge, directly grouped with the status label and headline.
- **Hero Card elevation** raised — now visibly more prominent than the
  cards beneath it.
- **Primary "Scan Now" button** — explicit, more generous height
  (`LayoutTokens.primaryActionHeight`, new token), reading as the one
  dominant action rather than a standard-weight button.
- **Security Summary** — stronger numeric emphasis, smaller labels, and
  a subtle semantic accent color on Threats Found specifically when
  its value is non-zero.
- **Quick Actions** — larger, more deliberate icon sizing.
- **Recent Activity** — fixed two real semantic mismatches found while
  polishing: the activity icon always showed a checkmark regardless of
  scan result, and its color was backwards (clean scans got the brand
  color, threats-found scans got neutral gray). Now shows the correct
  icon, correctly tinted.

### Not changed, deliberately

- No new statistics, no fabricated data, no layout restructuring — the
  Hero Card → Security Summary → Quick Actions → Recent Activity order
  and every section's underlying data are exactly as Sprint 036 left
  them.

See ADR 0051 for full reasoning.

## Sprint 036 — Home Screen Redesign (SDS Phase 2)

Presentation-layer only, built on Sprint 035's design system.
HomeViewModel, ScanViewModel, repositories, and the database were not
touched.

### Added

- **Hero Security Card** — protection status, last scan, and the scan
  action merged into one dominant surface, replacing three separate
  cards.
- **Security Summary**, **Quick Actions**, and **Recent Activity**
  sections.
- **Four real navigation shortcuts** from Home (Security Center,
  Cleaner, Scan History, Settings) — `HomeRoute` gained four callback
  parameters, wired in the nav host using the same pattern Security
  Center's own history callback already established. No new routes, no
  nav graph changes.
- **`AppStatCard`** (new, `core:ui`) — extracted from `ScanSummaryCard`'s
  own private stat-card pattern to avoid duplicating it in `HomeScreen.kt`.

### Not changed, deliberately

- "Apps Scanned" and "Scan Duration," shown in the reference design,
  are not persistent data anywhere in `HomeUiState`/`ScanUiState` — the
  Security Summary shows only the two stats that actually, reliably
  exist (Threats Found, Trusted Items), rather than extending the
  ViewModel or fabricating values to match a mockup.
- Recent Activity shows one real event (the last scan), not an invented
  multi-item feed — this project's architecture doesn't currently
  distinguish or persist the kind of granular events ("Database
  Updated," "Threat Removed") the reference design shows as examples.

See ADR 0050 for full reasoning, including both real conflicts between
the reference design and the actual data model.

## Sprint 035 — Space Design System v1.0, Phase 1 (Token Layer)

Design-token foundation only. No screen or component redesign — that's
scoped to a later phase. No ViewModel, navigation, Room, or business
logic touched.

### Added

- A complete, separated token layer in `core:designsystem/theme/`:
  brand colors, semantic colors, typography (full 15-slot M3 scale),
  shapes (raw scale + semantic component tokens), spacing, layout
  (screen padding, content max-width, touch targets, list-row heights),
  elevation (five named tiers), motion (named animation durations),
  icons (semantic mapping, one icon family), and component-state
  guidelines (Loading/Error).

### Changed

- **Dynamic color now defaults to off.** Previously on by default,
  meaning the deliberately-chosen brand color was never actually shown
  on a majority of the current Android install base.
- `StatusChip` and `ScanResultBadge` now consume `ShapeTokens.chip`/
  `ShapeTokens.badge` instead of each hardcoding the same shape
  independently.

### Not changed, deliberately

- The brand primary color stays teal, not the reference design's
  "Security Green" — that color was already chosen specifically to
  differentiate from competitors, and this sprint's own brief asks for
  an original identity, not a copy of them. Formalized as its own named
  semantic color instead of discarded.
- `Severity` stays at exactly three tiers. New color/state tokens
  (`Suspicious`, `ComponentState`) are defined for real, honest
  completeness without being wired to signals this project doesn't
  actually compute.

See ADR 0049 for the full token catalog and reasoning.

## Sprint 034 — Final Security Center UI Polish

UI/UX only, against a provided design mockup used as inspiration. No
scanning engine, analyzer, repository, or Room file touched — confirmed
via exact diff scope.

### Added

- **Scan Summary dashboard** — large status icon and message, last scan
  time, and a stat grid (apps scanned, findings, trusted, a severity
  breakdown, duration, highest severity, average confidence) replacing
  the previous plain labeled-field list.
- **Evidence rows** — each finding's evidence now shows as an icon,
  short title, and description, instead of a plain bullet list.
- **Recommendation card** — a light-background surface with an icon,
  replacing plain divider-separated text.
- **Animated expand/collapse** on threat cards.
- **A dedicated result badge for scan sessions** ("Safe," green) in
  History, distinct from the severity system.

### Fixed

- **Severity chips now carry an icon alongside their label**, and were
  relabeled to be clearer ("Informational," "High Risk").
- **Two real, pre-existing bugs found while doing this UI work**: both
  Security Center and History were missing a required parameter to the
  shared threat card component (a Sprint 033 regression that had gone
  unnoticed), and History's clean-scan badge was incorrectly reusing an
  "Informational" label for sessions with zero findings — now shows
  "Safe" correctly.

### Not changed, deliberately

- The reference design showed five severity colors; this app still uses
  exactly three. A fourth or fifth tier with no real underlying signal
  to justify it would be inventing distinctions the detection engine
  doesn't actually make.
- No new analyzer, scorer, or business logic anywhere in this sprint.

See ADR 0048 for full reasoning across all eight parts of this sprint.

## Sprint 033 — Professional Threat Report & Detection Quality

Five parts, all sharing one principle: everything derived at display
time from data already collected — no new persistence, no Room changes,
`CumulativeRiskScorer` unchanged, uninstall functionality untouched
(confirmed via full-file re-read, per this sprint's own "treat
uninstall as frozen" instruction).

### Added

- **Professional threat report format**: each finding now shows a
  Threat Category (`ThreatDescriptionProvider.categoryFor`) alongside
  its existing evidence, reason, and recommendation.
- **Four-tier confidence** (Very High / High / Medium / Low), replacing
  the raw three-tier internal value as what's shown to users. "Very
  High" specifically means two or more independent analyzers already
  agreed strongly enough to escalate the finding — a real, derived
  signal, not just a relabeling of one detection's own confidence.
- **Scan summary**: apps scanned, threats detected, trusted apps,
  ignored threats, scan duration, highest detected threat, and average
  confidence, shown above the threat list. Trusted apps and ignored
  threats are deliberately different numbers — apps skipped from
  analysis versus threats found and later marked trusted are genuinely
  different events.

### Improved (false-positive reduction)

- `SurveillanceCombinationAnalyzer` now recognizes `PRODUCTIVITY`-category
  apps (AI-assistant apps like ChatGPT) as a confidence-downgrade signal.
- `SuspiciousPermissionPatternAnalyzer`'s SMS rule now recognizes
  `VIDEO`-category apps (TikTok-style short-form video apps).
- A third trusted app store recognized: Xiaomi's own GetApps
  (`com.xiaomi.mipicks`) — Xiaomi Home and Mi Store previously had
  neither installer trust nor a matching category available.

### Not changed, deliberately

- No new analyzers. `CumulativeRiskScorer` untouched — every fix stays
  at the confidence-modulation layer established in Sprint 031.
- "Dynamic Code Loading" and "accessibility abuse" detection were not
  attempted — both require capabilities (bytecode analysis, service-level
  manifest inspection) this project genuinely doesn't have; treated as
  an honest scope boundary, not a silent gap.
- Binance's false-positive case remains the same "no Android finance
  category exists" limitation already documented for banking apps.

See ADR 0047 for full reasoning across all five parts.

## Sprint 032 — Context-Aware Detection Intelligence

The brief named three classes to review before making changes —
`PermissionCombinationAnalyzer`, `OverlayBehaviorAnalyzer`,
`SmsBehaviorAnalyzer` — none of which exist under those names in this
codebase. Verified against a fresh clone before any other work, per the
brief's own "do not assume anything" instruction, and mapped explicitly
onto the real classes (`SuspiciousPermissionPatternAnalyzer`,
`OverlayPermissionAnalyzer`, `SurveillanceCombinationAnalyzer`) rather
than guessed at silently. See ADR 0046.

### Improved

- **`OverlayPermissionAnalyzer`** now recognizes two more real,
  already-collected app categories as legitimacy signals: `AUDIO`
  (floating media-playback controls) and `MAPS` — Android's actual
  "Maps & Navigation" category — for navigation and ride-sharing apps
  showing turn-by-turn directions as an overlay.
- **`SurveillanceCombinationAnalyzer`** now treats `IMAGE`-category apps
  (camera apps, photo editors) as a confidence-downgrade signal —
  deliberately *not* added to its existing `VIDEO`/`SOCIAL` suppression:
  a photo editor doesn't inherently need microphone access the way a
  video-calling app does, so this is a genuinely weaker signal.
- **Recommendations** for camera/microphone, overlay, and SMS findings
  now each name several plausible legitimate app types (ride-sharing
  verification, navigation, banking authentication, and others) instead
  of one generic phrase — honestly, since the engine has no way to know
  which one specifically applies from evidence text alone.

### Not changed, deliberately

- No new analyzers, no UI changes, no Room schema changes.
- `CumulativeRiskScorer` needed no change — cross-analyzer reasoning is
  satisfied by the existing Sprint 031 escalation mechanism doing more
  useful work now that more analyzers recognize more categories
  correctly, not a new mechanism.
- `AppIdentityImpersonationAnalyzer` and `HighRiskPackageNameAnalyzer`
  are completely untouched — confirmed both structurally (their main
  files weren't modified) and behaviorally (new tests prove both still
  report their original, unmodified confidence even from a trusted app
  store).
- Banking, wearable-companion, and smart-home apps — also named in this
  sprint's brief — have no corresponding category in Android's own
  taxonomy, which `AppCategory` deliberately mirrors exactly rather than
  inventing categories the OS doesn't provide. Installer trust remains
  the only applicable signal for those app types; documented as a
  genuine platform limitation, not worked around.

See ADR 0046 for full reasoning, including the exact class-naming
discrepancy found before any code was touched.

## Sprint 031 — Detection Intelligence & Confidence Engine v2

Physical-device testing reported well-known, feature-rich apps
(communication, banking, Samsung, ride-sharing) reaching Action Needed
despite expected permission usage. Diagnosed from actual engine
behavior, not assumptions, before any change was made.

### Fixed

- **Legitimate apps over-escalated to Action Needed.** Three
  permission-behavior analyzers (SMS/device-admin, overlay, camera+mic+
  internet) always reported flat Moderate confidence with no awareness
  that feature-rich, legitimate apps commonly need several of their
  respective permission clusters at once. New `ConfidenceModulation`
  lowers confidence one tier when a real, already-collected, on-device
  signal suggests legitimacy — installed from a known app store (Google
  Play, Samsung Galaxy Store), or a declared app category consistent
  with the specific finding. `CumulativeRiskScorer`'s escalation rule
  itself is **unchanged** — it was already correct; it just wasn't being
  given confidence values that reflected everything already known about
  the app.

### Added

- Contextual "why might this still be legitimate" explanations,
  automatically appended when every finding behind a card has been
  confidence-downgraded.
- A visible confidence indicator (Low/Moderate/High) in each card's
  expanded detail, alongside the recommendation.

### Not changed, deliberately

- `AppIdentityImpersonationAnalyzer` and `HighRiskPackageNameAnalyzer`
  do not use confidence modulation — an app claiming a false identity or
  squatting a reserved namespace has no legitimate excuse regardless of
  app store or category.
- `SurveillanceCombinationAnalyzer`'s existing category suppression
  (video-calling apps) is untouched.
- No analyzer removed, no detection capability reduced. A genuinely
  suspicious app with no legitimacy signal still escalates exactly as
  before — verified with the exact same existing test, left unmodified,
  passing alongside a new one proving the fix.
- No new Room schema, no new `InstalledApplicationInfo` fields — both
  signals used were already collected in prior sprints.

See ADR 0045 for full reasoning, including an honest note on the one
area (a Samsung Galaxy Store package name) not verified against a live
device in this sandbox.

## Sprint 030 — Security Center UI/UX Modernization

Rebuilds the Security Center and History screens into a polished,
card-based design — inspired by AVG Protection's layout, implemented as
an original Material 3 design, not a copy. No detection logic, scan
engine, or analyzer changed.

### Added

- **`ThreatSummaryCard`** (`core:ui`), the shared component behind both
  screens: severity-colored accent edge, real app icon (loaded via
  `PackageManager` at display time, with a letter fallback for
  uninstalled apps), app identity shown before any explanation, a
  compact evidence-icon row, a collapsed-by-default short summary, and
  an expandable section (technical explanation, full evidence list,
  recommendation) behind a "View details" toggle.
- **Real actions on every card**: "Ignore" now genuinely marks the app
  trusted (`AddTrustedItemUseCase`, existing since Sprint 008, never
  previously connected to any UI); "Open app info" and "Uninstall" both
  launch real, standard, permission-free Android system intents.
- **Contextual recommendations and short summaries**, based on which
  evidence is actually present and how severe the finding is — not
  identical text for every app.

### Fixed

- **The severity chip that looked clickable but did nothing.** It was a
  Material3 `AssistChip` with an empty `onClick` — inherently
  interactive regardless of the lambda's content. Rewritten as a
  component with no `onClick` parameter at all.
- History now shares the same card design as Security Center, instead
  of its own older, plainer layout.

### Not changed

- No new `Detection`/`Threat` fields, no Room schema change. Evidence
  icons and contextual copy both infer from evidence text this project
  already controls the wording of; app icons load fresh rather than
  being persisted.

See ADR 0044 for full reasoning, including the one area not verified
against a real compiler in this sandbox (exact extended Material icon
names — isolated to a single file if any need correcting).

## Sprint 029 — Report Quality & Deduplication

Real-device testing reported the scan report looking harder to read than
Sprint 027's despite better detection logic. Traced to the actual root
cause before any fix was written: `Threat` never carried the app's
display name, so different apps sharing a generic finding category
looked identical in the list — not literal duplication, which the
architecture already prevented (verified directly, not assumed).

### Fixed

- **Apps indistinguishable in the report.** `Threat.appLabel` is now a
  real field, populated end to end from the app's actual name. Scan
  report cards now show the application's name and package before any
  explanation.
- **Verbose, repetitive explanations.** Every analyzer's evidence text
  shortened to 1-2 concise sentences. The report now shows one short
  bullet per finding instead of one long concatenated paragraph.
- **No clear recommendation.** New `ThreatDescriptionProvider.recommendationFor()`
  — a short, separate, actionable line per finding category, shown in
  its own section.
- **A second, real, pre-existing bug found while extending this exact
  schema:** `Detection.confidence` (Sprint 027) was never actually
  persisted to Room — silently reset to its default on every read.
  Fixed alongside the main work.

### Changed

- `SecurityCenterScreen`'s report cards restructured: application
  identity first, then risk, then evidence (as bullets), then
  recommendation — replacing one long paragraph per finding.
- Room schema version 3→4 (`ThreatEntity.appLabel`,
  `DetectionEntity.confidence`), using this project's existing
  destructive-migration policy for its pre-1.0 state.

### Not changed

- `feature:history` retains its own separate, older report layout for
  now — a real, deferred next step, not a silent gap.

See ADR 0043 for full reasoning.

## Sprint 028 — Threat Intelligence Refinement & Confidence Engine

Real-device testing of Sprint 027's eight analyzers reported five
concrete quality issues. This sprint fixes three, adds zero new
analyzers, and touches nothing outside `core:model`, `core:enumeration`,
and `core:analysisengine` — no UI, no navigation, no regressions to
existing analyzers, confidence scoring, severity model, threat
explanations, scan performance, or the Sprint 027 compile/Windows
compatibility fixes.

### Fixed

- **`HighRiskPackageNameAnalyzer` false-flagging legitimate Google
  apps.** `com.google.android.` is no longer treated as a reserved
  system namespace — many genuine, Play-Store-distributed Google apps
  (Gmail, YouTube, Maps) use it, and `isSystemApp` (the analyzer's only
  prior protection) is commonly `false` for exactly these apps once
  updated via the Play Store. `com.android.` and `android.` remain
  reserved and continue to work as before.
- **Repetitive findings on the same app.** `DeviceAdministratorAnalyzer`
  now also excludes apps with `INTERNET` permission — it was overlapping
  with `SuspiciousPermissionPatternAnalyzer`'s existing device-admin+
  INTERNET combo rule (Sprint 014), producing two differently-worded
  findings about the same underlying fact. This analyzer's own purpose
  is catching device-admin apps the combo rule can't see; once an app
  has both permissions, the combo rule already covers it more
  specifically.
- **No contextual awareness for expected permission combinations.**
  `SurveillanceCombinationAnalyzer` now skips apps declaring
  `ApplicationInfo.CATEGORY_VIDEO` or `CATEGORY_SOCIAL` entirely, rather
  than flagging them with softened wording — a video-calling app
  legitimately needing camera+microphone+internet isn't suspicious, and
  not flagging it is more honest than flagging it with a caveat.

### Added

- `AppCategory`, mapped from `ApplicationInfo.category` (stable since
  API 26, this project's exact `minSdk`) via its own named constants.
  `InstalledApplicationInfo.category` defaults to `UNDEFINED`, populated
  in the existing enumeration pass — no new `PackageManager` call.

### Not changed

- No new analyzers, per the real-device report's own instruction to
  improve false-positive resistance before adding more.
  `AppIdentityImpersonationAnalyzer` and `AnalysisOutcomeAggregator`
  were both reviewed and deliberately left as-is — see ADR 0042 for why.

See ADR 0042 for full reasoning, including independent verification of
the two "integration fixes" this sprint's brief described as already
merged, before treating them as baseline.

## Sprint 027 — Intelligent Threat Detection Engine v2

### Added

- **Six new production threat analyzers** (eight total, up from two):
  `OverlayPermissionAnalyzer` (SYSTEM_ALERT_WINDOW + INTERNET),
  `SurveillanceCombinationAnalyzer` (CAMERA + RECORD_AUDIO + INTERNET),
  `DeviceAdministratorAnalyzer` (standalone device-admin flag),
  `HighRiskPackageNameAnalyzer` (non-system apps claiming a reserved
  Android system namespace), `DebuggableApplicationAnalyzer`
  (release-build debuggable flag), and `UnknownInstallerSourceAnalyzer`
  (unrecognized install provenance, the most conservative analyzer in
  the project — LOW confidence by design). Every analyzer produces real,
  evidence-based findings from actual `PackageManager` data — no demo or
  hardcoded output.
- **`Confidence`** (LOW/MODERATE/HIGH), a new axis on `Detection`
  distinct from `RiskLevel` — how sure an analyzer is about its finding,
  separate from how severe the finding would be if true.
- **`CumulativeRiskScorer`**, replacing `HighestSeverityRiskScorer` as
  the active scoring strategy (which remains in the project as a valid,
  tested alternative). Two or more independent analyzers each
  contributing an ATTENTION+/MODERATE+ finding on the same app now
  escalate that app's overall risk to ACTION_NEEDED — the "Accessibility
  + Overlay → high confidence, not two unrelated warnings" behavior this
  sprint asked for, precisely defined and tested at its boundaries.
- New `ThreatType.SUSPICIOUS_APP_CONFIGURATION` category for findings
  about how an app is built or installed, distinct from what permissions
  it requests or who it claims to be.
- `InstalledApplicationInfo.isDebuggable` and `.installerPackageName`,
  populated within the existing single `PackageManager` enumeration
  pass — no new redundant calls introduced.

### Changed

- Duplicate threat elimination required no new merging logic — it's a
  natural consequence of the per-app aggregation pipeline that has
  existed since Sprint 004C. Verified directly with a new end-to-end
  test: an app matching three analyzers at once produces exactly one
  merged `Threat`, not three.
- Scan results now naturally vary by device, since they always have —
  every analyzer operates on the real, installed-app data of the device
  running the scan. No device-detection logic was added or needed.

See ADR 0041 for full reasoning, including which three of the ten
candidate analyzers (Accessibility Service abuse, excessive background
permissions, and VPN applications) were deliberately not attempted this
sprint — the first and third need manifest-`<service>`-level data
collection this sprint's scope didn't allow building with real
confidence; the second was set aside to keep this sprint's scope
proportionate rather than attempted superficially.

## Sprint 026.1 — Hotfix

Real-device testing of Sprint 026 (Samsung Galaxy S9+ / Android 9 API 28,
Xiaomi 23053RN02A / Android 15 API 35) surfaced two issues before Sprint
026 could be considered production-ready.

### Fixed

- **Background Protection failed to schedule on real devices** —
  enabling the switch on Settings caused WorkManager to throw
  `NoSuchMethodException: ScanWorker.<init>(Context, WorkerParameters)`
  when attempting to construct `ScanWorker`. Root cause: WorkManager's
  default `ContentProvider`-based auto-initializer runs before
  `Application.onCreate()` on every Android version, so it could read
  `SpaceAntivirusApp`'s `Configuration.Provider` implementation before
  Hilt had field-injected `HiltWorkerFactory`, silently falling back to
  WorkManager's own non-Hilt-aware default factory. Not an Android-9-
  specific bug — the ordering issue is constant across API levels, it
  was simply the first device this was exercised on. Fixed by disabling
  WorkManager's default auto-initializer (manifest `<meta-data>`
  override) and calling `WorkManager.initialize(...)` manually from
  `Application.onCreate()`, which Hilt's generated base class always
  invokes after field injection completes. See ADR 0040.

### Investigated

- **~2 second startup frame skip** reported on Android 15. Audited
  `MainActivity`, `SpaceAntivirusApp`, `DataModule` (Room/DataStore
  providers), and `OnboardingViewModel` (the app's actual start
  destination) for synchronous main-thread work reachable from cold
  launch; found none — Room's `databaseBuilder().build()` and the
  DataStore delegate are both lazy by construction. Considered
  backgrounding the new `WorkManager.initialize()` call as a possible
  contributor, but reverted that approach: `RECEIVE_BOOT_COMPLETED`
  means this `Application` can launch as part of a boot-triggered start,
  and deferring initialization asynchronously would risk WorkManager's
  own bundled boot-rescheduling receiver firing before the deferred call
  completes — a real correctness regression traded for an unverified
  performance gain. Root-causing the full duration precisely needs live
  on-device profiling this environment cannot perform; no change was
  made here that couldn't be verified with real confidence.

## Sprint 026 — Production Settings screen

Exposes the background scan scheduling infrastructure built in Sprints
024–025 through a real Settings screen: a toggle to enable/disable
Background Protection and interval selection (Daily / Every 3 Days /
Weekly), persisted via a new `BackgroundProtectionPreferences` domain
contract backed by the existing DataStore-based preferences
infrastructure. See ADR 0039.
