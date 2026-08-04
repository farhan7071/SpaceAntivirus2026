# ADR 0055: Background Protection Engine (Sprint 042)

**Status:** Accepted

## Context

Sprints 024/025 built the scheduling foundation: a `BackgroundScanScheduler`
contract, a WorkManager implementation with unique periodic work, battery
and storage constraints, and persisted preferences. Sprint 026 wired
Settings to it. What did not exist: any notification of any kind, a
protection surface on Home, boot-time restoration of user-visible state,
a notify-after-scan preference, or any battery-optimisation awareness.

Sprint 042 adds those. It also had to decide what a background
protection engine is allowed to *claim*.

## The claim problem

The sprint brief specified this copy for the ongoing notification and the
Home card:

> Protection Enabled / **Real-time protection active**

This app has no real-time protection. It runs scheduled scans. Live file
scanning, APK interception, accessibility monitoring, install
interception, VPN and network firewall are all explicitly out of the same
brief's scope, and none of them exist anywhere in the project.

A permanent notification claiming real-time protection would be a false
security claim sitting in the user's shade indefinitely — the most
consequential place this project could put one. ADR 0015's "never
exaggerate risk" rule is usually invoked against overstating danger; it
applies just as much to overstating protection, and arguably more, since
a user who believes they have real-time protection may take risks they
otherwise wouldn't.

**Decision:** the notification and the Home card say what is true —
scheduled scanning is on, and roughly when the next scan is due. The
brief's copy was not used.

The same reasoning produced the word "around" in the next-scan line.
WorkManager decides when periodic work actually fires and defers it for
the battery and storage constraints this project sets, so "next scan at
14:30" would state a guarantee the platform does not make.
`ProtectionState.earliestNextScanEpochMillis` is named accordingly.

## ProtectionManager, and why the ordering is structural

Enabling protection is three steps that must happen in one order:

1. Ask the scheduler to enqueue the work.
2. Only if that succeeded, persist the new state.
3. Only then, post the notification.

Any other order produces a lie. Persisting first means preferences claim
protection is on when WorkManager rejected the request. Notifying first
means the user is told their device is being monitored before anything is
scheduled to monitor it. Sprint 024/025 established this invariant, but
enforced it *by convention* inside `SettingsViewModel`, which was fine
while Settings was the only caller.

Sprint 042 adds three more callers — Home's quick toggle, the boot
receiver, and the worker reporting its own result — and three callers
each re-deriving the same ordering is how they drift. `ProtectionManager`
owns it now, and every caller gets one method.

The disable path deliberately inverts step 3: the notification is removed
*first*. If cancellation then fails, a stale "protection active"
notification left on screen is the worse of the two outcomes.

`ProtectionManagerImpl` contains no Android framework types at all, which
is what lets the ordering — including every failure path — be tested on
the JVM.

## Deletions

Six use cases were deleted: `ScheduleBackgroundScanUseCase`,
`CancelBackgroundScanUseCase`, `RecordBackgroundProtectionEnabledUseCase`,
`RecordBackgroundProtectionDisabledUseCase`,
`ObserveBackgroundProtectionEnabledUseCase` and
`ObserveLastScheduledAtUseCase`. Once `SettingsViewModel` moved to
`ProtectionManager`, all six had zero production callers — they existed
only to be composed in the exact order the manager now guarantees.
Keeping them would leave a second, unordered route to the same
repositories, which is the thing this sprint exists to prevent.
`BackgroundScanScheduler`, `BackgroundProtectionPreferences` and
`SetScanIntervalUseCase` are unchanged.

## The boot receiver does not reschedule

`BootCompletedReceiver` restores the ongoing notification and nothing
else.

WorkManager already reschedules its own persisted periodic work after
boot, through a receiver merged automatically from the androidx.work
AAR — that is what the manifest's `RECEIVE_BOOT_COMPLETED` permission has
been for since Sprint 025, documented there at the time. Enqueueing again
here would replace a live schedule and reset its interval window, so a
user who reboots frequently would see scans repeatedly pushed back. The
work survives reboot without our help.

What does not survive is the ongoing notification, which the system
clears. Without this receiver, a user who restarts their phone would find
protection still working but the notification saying so gone — which
reads as protection having silently switched itself off.

## Notifications

Three channels, at three genuinely different importances:

- **Protection status** — LOW. A persistent status line should never make
  a sound.
- **Scheduled scan results** — DEFAULT.
- **Security alerts** — HIGH, created so the user can configure it, and
  deliberately **not posted to by anything in this sprint**. A
  high-importance alert channel is for something urgent the user has not
  already been shown. Every finding this app can currently produce comes
  from a scan the user either started themselves or has already been told
  about through the scheduled-scan channel. Posting the same information
  a second time at higher urgency would be manufacturing alarm rather
  than conveying it.

**Notify-after-scan defaults to off.** A security app that pings after
every routine scan that found nothing trains the user to dismiss it, and
a notification the user has learned to ignore is worse than no
notification at all when something genuinely needs attention.

**POST_NOTIFICATIONS is a real gap this sprint surfaces.** The manifest
has declared it since Sprint 003, but on API 33+ it is a runtime
permission and this project has never requested it — so on any modern
device it is denied by default and every notification here will silently
not appear. `NotificationHelper` checks and no-ops rather than throwing,
and `areNotificationsPermitted()` exposes the real answer. Wiring the
runtime request into onboarding is left for a follow-up sprint rather
than bolted onto this one, because where to ask matters: a permission
prompt fired the instant a user flips a toggle is the pattern that gets
denied.

## Battery optimisation

Detected via `PowerManager.isIgnoringBatteryOptimizations`, surfaced as
one informational card, and acted on by nobody. This app never requests
the exemption: `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is
Play-restricted to a narrow set of app categories, and a security app
nagging its way onto an unrestricted battery allowlist is exactly the
behaviour that gives this category its reputation. The card links to the
system's own settings list, which needs no permission, and the copy is
careful not to overstate the benefit — the check sees the standard
allowlist only, and several manufacturers layer their own process
management on top of it that it cannot see.

## Consequences

- Protection is one object with one owner, and its ordering guarantees
  are tested rather than conventional.
- The app tells the truth about what it does in the one place users are
  most likely to over-read it.
- Notifications will not appear until POST_NOTIFICATIONS is requested at
  runtime. That is a known, documented gap, not a silent one.
