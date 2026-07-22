# ADR 0001: Use Hilt for dependency injection

**Status:** Accepted

## Context
Sprint 002 §7 called for a DI approach usable from Compose ViewModels across ~20 modules. Sprint 001 found some existing Dagger presence in the obfuscated original codebase, suggesting a compatible migration path rather than a cold start.

## Decision
Use Hilt across all modules that need DI (every core:* and feature:* module except :domain and :core:model, which have none). hiltViewModel() scopes ViewModels to nav destinations, matching current (2026) Compose guidance.

## Consequences
Every module needing Hilt applies the spaceav.android.hilt convention plugin (adds KSP + hilt-android). :app carries @HiltAndroidApp. Tradeoff: KSP annotation processing adds build time vs. a manual-DI approach, accepted for the testability and compile-time-safety gains.
