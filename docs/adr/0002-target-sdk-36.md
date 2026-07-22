# ADR 0002: Target SDK 36 from the start of the rebuild

**Status:** Accepted

## Context
Sprint 001 found the original app at target SDK 33, one full requirement cycle behind. Sprint 002 §9 noted Play requires 35 by Aug 31 2026 for existing apps and 36 for any new submission after that date.

## Decision
Build against compileSdk/targetSdk 36 from Sprint 003 onward rather than targeting 35 first and upgrading again later.

## Consequences
Avoids a second SDK-bump pass later. Means Android 14/15/16 background-execution and foreground-service rules apply from day one — directly resolves Sprint 001 Risk #2 by forcing the foreground-service-type decision (ADR 0010, deferred to Sprint 004 with feature:realtime) before any real-time-protection code ships.
