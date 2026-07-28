# Changelog

All notable changes to this project are documented here. Format loosely
follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); this
file starts with Sprint 026's real-device hotfix rather than
retroactively documenting every prior sprint, since ADRs already serve
as this project's detailed historical record (`docs/adr/`).

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
