# ADR 0004: Feature modules never depend on each other

**Status:** Accepted

## Context
Sprint 001 found the original app's 'engine' (MAK Antivirus) and 'shell' (app UI) tightly interwoven with no clean seam. Sprint 002 §7 explicitly named this as the architectural fix target.

## Decision
Every feature:* module may depend on core:* and :domain, never on another feature:* module. Cross-feature navigation happens only through the :app-level NavHost via route string constants, never through a direct Kotlin dependency.

## Consequences
Enforced by convention (spaceav.android.feature plugin only wires core:*/domain dependencies) rather than a build-time check in Sprint 003 — a Gradle dependency-graph lint check (e.g. via a custom Gradle task) is a reasonable Sprint 004 addition once real cross-feature navigation cases exist to test against.
