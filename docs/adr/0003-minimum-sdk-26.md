# ADR 0003: Set minSdk to 26 (Android 8.0)

**Status:** Accepted

## Context
The original app supported minSdk 24 (Sprint 001). Sprint 002 didn't set an explicit floor. A modern Compose + Material3 + Hilt + WorkManager stack has meaningfully better behavior on 26+ (notification channels, adaptive icons, background limits already present).

## Decision
Raise the floor to API 26. Below-26 device share is now a small minority of the addressable market and the simplification (no need to special-case pre-Notification-Channel code paths, for example) outweighs the reach loss.

## Consequences
Re-verify actual minSdk-24-vs-26 install-base impact using real Play Console statistics before Sprint 004 feature work locks this in further — this is a reversible decision, but should be confirmed with real data rather than left as an assumption.
