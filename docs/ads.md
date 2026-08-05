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

**No build currently serves ads.** `ConsentProvider` is bound to
`UnresolvedConsentProvider`, which returns `ConsentState.UNKNOWN`, which
the gate refuses.

That is deliberate, not unfinished. Serving personalised ads to a user in
the EEA or UK without a Google-certified consent platform breaches
Google's EU User Consent Policy. A seam that failed *open* would mean the
app was in breach from the moment real ad unit IDs were pasted in. Failing
closed means the worst case of shipping before UMP lands is no revenue
rather than a policy violation.

To enable ads: integrate the UMP SDK, implement `ConsentProvider` against
it, and change one `@Binds` in `AdsModule`. Nothing else moves.

## Manual steps before release

1. **AdMob app ID** — replace the sample value in
   `app/src/main/AndroidManifest.xml`
   (`com.google.android.gms.ads.APPLICATION_ID`). The current value is
   Google's published sample ID: permanently available, test ads only, no
   revenue, no invalid traffic. Shipping a *live* app ID in a debug build
   is a common route to an account suspension, which is why the safe
   value is the default.
2. **Ad unit IDs** — replace `BANNER_AD_UNIT_ID` and
   `INTERSTITIAL_AD_UNIT_ID` in `AdsConfig`. Both are currently Google's
   published test units.
3. **UMP consent** — see above. Until this is done the app serves nothing.
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
