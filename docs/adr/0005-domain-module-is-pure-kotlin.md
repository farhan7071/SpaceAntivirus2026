# ADR 0005: domain module has zero Android framework dependency

**Status:** Accepted

## Context
Sprint 002 §7 specified UseCases as the layer that coordinates multiple Repository calls, and current (2026) Android architecture guidance treats ViewModels/UseCases as plain-Kotlin-testable by design.

## Decision
`:domain` applies only the Kotlin JVM plugin, never the Android Library plugin. Its UseCase base classes take a CoroutineDispatcher as a constructor parameter rather than referencing Dispatchers.IO directly, so tests can substitute a TestDispatcher with no Robolectric/instrumented-test overhead.

## Consequences
Use cases run in plain `./gradlew test` (fast, JVM-only) rather than `connectedAndroidTest`. Tradeoff: domain types (Sprint 004+) must not reference any android.* type — this is a real constraint feature authors need to know, documented in docs/coding-standards.md.
