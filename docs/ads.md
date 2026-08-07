# Ads architecture and release checklist (Sprint 044)

## Shape

```
core:ads                      the ONLY module that may reference the
                              Google Mobile Ads SDK
  AdsConfig                   every ad unit ID and flag, in one file
  AdPlacement                 closed enum of permitted placements
  AdsGate                     pure-Kotlin policy: consent, grace period,
                              frequency cap. Fully unit tested.
  ConsentState/ConsentProvider  the UMP seam. Defaults to UNKNOWN.
  AdsController               the app's whole ad surface: 4 methods, no
                              SDK types in any signature
  GoogleAdsController         the one SDK-touching class
  NoOpAdsController           production binding whenever ads are off
  LocalAdsController          composition local, defaults to NoOp
  ui/AdBanner                 Compose banner; emits nothing when refused

app        initialises the SDK, holds the Activity the SDK needs,
           provides LocalAdsController at the root
feature:history   the only feature module depending on core:ads
```

No feature module imports a Google class. `Activity` appears in exactly
one interface method, because the SDK genuinely requires one to present
a full-screen ad.

## Placement strategy

Two placements exist, and the enum is closed so a third requires editing
that file.

**`HISTORY_BANNER`** — beneath the scan history list. History is passive,
scrollable, reached deliberately, and carries nothing the user is acting
on urgently. The banner sits *below* the list, never above it: a banner
between a user and their own scan results is what makes a security app
feel like adware.

**`SCAN_COMPLETE_INTERSTITIAL`** — after a manual scan finishes *and* the
user has dismissed the result. The dismissal is the point. Showing this
the instant a scan completes would cover the answer the user asked for,
which is both a Play policy risk and the most user-hostile thing an
antivirus app can do.

Deliberately absent: onboarding, in-progress scans, Security Center
findings, and the Cleaner's deletion flow. Those are the moments a user
is deciding whether to trust this app, or acting on something they have
been told is a risk.

The sprint brief also proposed interstitials on opening the Cleaner and
on viewing Scan History. Both were declined. An interstitial fired by
navigation is unexpected by definition, and three interstitial triggers
plus a banner in an app this size is the clutter the same brief asked to
avoid. Adding either later is a one-line change to `AdPlacement` plus a
call site — deliberately easy, deliberately not automatic.

## Initialisation flow

1. `SpaceAntivirusApp.onCreate` calls `adsController.initialize()`,
   **after** `WorkManager.initialize` — boot-triggered starts need
   WorkManager promptly (ADR 0040) and nothing about ads is time-critical.
2. `initialize()` returns immediately as a no-op in debug builds. In
   release it calls `MobileAds.initialize`, which completes its own I/O
   on a background thread, and starts the first-run grace-period clock.
3. `MainActivity.onCreate` preloads the interstitial. Preloading is a
   no-op whenever the gate would refuse the placement, so a blocked user
   spends no data.
4. Every ad request passes `AdsGate` first.

## The gate

Four questions, any "no" is final: ads enabled for this build → consent
resolved → past the first-run grace period → past the frequency cap.
Banners skip the last two; they are ambient rather than interruptive, and
a capped banner would leave an empty box that looks broken.

Only a genuinely *displayed* interstitial consumes the quiet period. A
failed load must not cost the user their next eligible moment.

## Consent — read this before shipping

**Sprint 049 integrated the UMP SDK.** `ConsentProvider` is now bound to
`UmpConsentManager`; the blocking placeholder is deleted. As Sprint 044
predicted, it was one `@Binds` change — the seam did its job.

**Order of operations, which is the part the policy actually governs:**

1. `MainActivity.onCreate` calls `gatherConsentAndInitialize(this)`.
2. UMP determines whether consent is required for this user's region,
   presents Google's certified form if so, and persists the outcome in
   its own storage.
3. Only if `canRequestAds()` comes back true is `MobileAds.initialize`
   called at all.

Sprint 044 initialised the ads SDK in `Application.onCreate` and gated
the *requests*. That is not sufficient: the EU User Consent Policy
governs initialisation, not merely the request. It also could not have
worked — UMP needs an Activity to present a form on, which an Application
does not have.

**`canRequestAds()` is the signal, not `ConsentStatus`.** Mapping
`OBTAINED` to yes and everything else to no is wrong in both directions:
a user outside the EEA gets `NOT_REQUIRED` and may lawfully see ads,
while `OBTAINED` alone does not distinguish someone who accepted
personalised ads from someone who declined them but remains eligible for
non-personalised ones.

**Every failure path is still fail-closed.** No network on first launch,
a form that will not load, a dismissed form — all leave the state at
`UNKNOWN`, which the gate refuses.

### Testing the EEA path

Register your device in the AdMob console under Privacy & messaging >
Test devices, and set its geography there. `ConsentDebugSettings` is
deliberately *not* used in this codebase: it forces a geography in code,
and a forced geography that reached a release build would be a
compliance hazard.

### Still outstanding: the privacy options entry point

Where `privacyOptionsRequirementStatus` is `REQUIRED`, Google requires an
ongoing way for the user to change their consent decision.
`UmpConsentManager.arePrivacyOptionsRequired()` and
`showPrivacyOptionsForm()` are implemented and ready, but **no screen
calls them yet** — Sprint 049's brief scoped UI changes out.

This is a real compliance gap for EEA users, not a nicety. The fix is one
`SettingsRow` in the Settings hub's existing Privacy section, shown when
`arePrivacyOptionsRequired()` is true.

## Production configuration (Sprint 047)

App ID, banner unit and interstitial unit are now the live Zx Force Soft
values. `AdsConfig.adsEnabled` keeps ads off entirely in debug builds,
checked against the installed package's real debuggable flag — a
developer build requests nothing at all, which is stronger protection
against invalid traffic than the test units it replaces.

**Two supplied ad units are deliberately not wired: Native and App
Open.** Neither has an implementation, and adding one would be a feature,
not a configuration change. App Open in particular is worth a decision
rather than a default: it shows before a user reaches any content, which
is the pattern Google's own placement guidance treats as disruptive, and
in a security app the first thing a worried user sees would be an ad.

## Manual steps before release

1. ~~AdMob app ID~~ — done in Sprint 047.
2. ~~Ad unit IDs~~ — done in Sprint 047.
3. **UMP consent — STILL BLOCKING, and the most important item here.**
   `ConsentProvider` is bound to `UnresolvedConsentProvider`, which
   returns `UNKNOWN`, which the gate refuses. **The app will serve zero
   ads in production until the UMP SDK is integrated.** Swapping in
   production ad unit IDs did not change this and was never going to:
   the block is by design, because serving personalised ads to an EEA or
   UK user without a certified consent platform breaches Google's EU User
   Consent Policy. Integrating UMP is one `@Binds` change in `AdsModule`
   plus the SDK itself.
4. **Play Data Safety declaration** — the Mobile Ads SDK collects device
   and advertising identifiers and approximate location. The Data Safety
   form must be updated to declare this before the next release, and the
   privacy policy must describe it. This is the single most likely thing
   to be forgotten and the most likely to cause a review rejection.
5. **Privacy policy** — `SupportLinks` still holds placeholder `.invalid`
   URLs (Sprint 043A). A published policy is mandatory for any app
   serving ads, so this is now blocking rather than cosmetic.
6. **Dependency version** — `playServicesAds` in the version catalog was
   set without network access to check for a newer release. Verify
   against the Play Console-recommended minimum.
7. **Families policy** — if this app is ever listed as child-directed,
   interstitials and personalised ads carry additional restrictions.
   Nothing here assumes a families listing.

## What the About screen now says

Adding an ads SDK made an existing line misleading by omission. The
About screen previously said "Nothing about your apps or files is
uploaded" without qualification. That statement was, and is, true of
*scanning* — but on a screen a user reads to decide whether this app is
nosy, it cannot stand unqualified while an ad SDK collects device
identifiers. The claim is now scoped ("Scanning is on-device only") and a
second row states the ads position plainly, including what the ad
provider does *not* receive: scan results, file names, or the installed
app list.
