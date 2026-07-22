# ADR 0009: Plain String route constants, not type-safe Navigation-Compose routes

**Status:** Accepted

## Context
AndroidX Navigation's newer type-safe (kotlinx.serialization-based) route API needs a specific Navigation-Compose version this sandbox cannot verify resolves against Maven (network restriction — see README). Sprint 003's navigation skeleton (Task 4) needs to exist regardless.

## Decision
Use plain `const val XNavigationRoute = "x"` per feature module for Sprint 003's skeleton.

## Consequences
Migrating to type-safe routes is a contained, mechanical Sprint 004 follow-up (touches only the NavHost wiring and each feature's route constant, not screen/ViewModel code) — tracked explicitly rather than silently left as permanent tech debt.
