# ADR 0056: UMP Consent Integration (Sprint 049)

**Status:** Accepted

## Context

Sprint 044 built the ads stack behind a `ConsentProvider` seam whose only
implementation returned `UNKNOWN`, which the gate refuses. That was
deliberate: serving personalised ads to a user in the EEA or UK without a
Google-certified consent platform breaches the EU User Consent Policy,
and a seam that failed *open* would have put the app in breach the moment
real ad unit IDs were pasted in — which Sprint 047 then did.

Sprint 049 puts Google's User Messaging Platform behind that seam.

## Decisions

### Initialisation moved from Application to Activity

Sprint 044 called `MobileAds.initialize` in `Application.onCreate` and
gated ad *requests* afterwards. Sprint 049 removes that call entirely.
The SDK is now initialised only after UMP reports that ads may be
requested.

Two reasons, and the second is decisive:

1. The policy governs initialisation, not merely the request.
2. UMP needs an `Activity` to present a form on. An `Application` does
   not have one, so the old placement could not have hosted the consent
   flow even if the policy had allowed it.

`MainActivity.onCreate` is the new caller. It is fire-and-forget: nothing
in the UI waits on consent, and a user whose consent is unresolved or
refused simply sees no ads.

### `canRequestAds()`, not `ConsentStatus`

The obvious mapping — `OBTAINED` means yes, everything else means no —
is wrong in both directions. A user outside the EEA gets `NOT_REQUIRED`
and may lawfully be shown ads. A user who declined personalised ads may
still be eligible for non-personalised ones, and `OBTAINED` alone does
not distinguish the two.

Google exposes `canRequestAds()` because it folds all of that in, and it
is the value the Mobile Ads SDK itself respects. The mapping is extracted
to a one-line internal function so it can be tested on the JVM without
the SDK.

### Consent is stored by UMP, and nowhere else

UMP persists the outcome in its own storage, surviving restarts and
shared with the Mobile Ads SDK. This project deliberately stores nothing
about consent. A second copy would be a second source of truth that goes
stale the instant a user changes their decision through the privacy
options form — the same reasoning that kept per-channel notification
control in Android's hands (ADR 0055) rather than mirrored in Settings.

### Every failure path stays fail-closed

No network on first launch, a form that fails to load, a form the user
dismisses — all leave the state at `UNKNOWN`, which `AdsGate` refuses.
The callback fires exactly once on every path including failure, because
a path that never called back would leave ads permanently uninitialised
rather than merely unserved.

### No `ConsentDebugSettings`

The SDK offers a way to force a geography for testing. It is not used
here. A forced geography that reached a release build would be a
compliance hazard, and testing the EEA path is properly done by
registering a test device in the AdMob console, which cannot leak into a
build.

## Consequences

- The app can serve ads in production for the first time.
- Debug builds still ask nothing and initialise nothing: there is no
  consent question worth putting to a developer whose build will never
  request an ad.
- **One compliance gap remains.** Where
  `privacyOptionsRequirementStatus` is `REQUIRED`, Google requires an
  ongoing way for the user to change their decision.
  `arePrivacyOptionsRequired()` and `showPrivacyOptionsForm()` are
  implemented, but no screen calls them, because this sprint's brief
  scoped UI changes out. The fix is one `SettingsRow` in the existing
  Privacy section. This is a real gap for EEA users, not a nicety.
