# ADR 0011: core:common and core:model are pure-Kotlin JVM modules, not Android Library modules

**Status:** Accepted

## Context
Sprint 003 originally applied `spaceav.android.library` (the Android Library
Gradle plugin) to `core:common` and `core:model`, even though neither
module's actual code touches any Android framework API — `core:common`
holds `AppResult`/`AppError` (plain sealed interfaces) and
`DispatcherProvider` (a `javax.inject`-annotated class using only
`kotlinx.coroutines` types); `core:model` was empty by design.

Meanwhile `:domain` correctly applies `spaceav.jvm.library` (the plain
Kotlin JVM plugin, per ADR 0005) and depends on both `core:common` and
`core:model`. A module using the Kotlin JVM plugin produces and consumes
plain `.jar`-style outputs; a module using the Android Library plugin
produces an `.aar` with Android-specific variant metadata (debug/release,
min/target SDK, etc.). Gradle's dependency resolution cannot match a
JVM-library consumer against an Android-library producer's variants — this
is a fatal, configuration-time failure, not a runtime or logic bug. It
would have surfaced the moment anyone ran `./gradlew build` or even
`./gradlew :domain:compileKotlin`, which is exactly what happened when the
Sprint 003 report's claimed validation was checked against a real build
attempt (see the Sprint 003.5 Engineering Recovery Report).

## Decision
Convert `core:common` and `core:model` to apply `spaceav.jvm.library`
instead of `spaceav.android.library`. Since coroutines' `Dispatchers.Main`
needs an Android-specific runtime implementation to actually function (not
just compile) inside the real app, that dependency (`kotlinx-coroutines-android`)
moves to `:app` itself, while `core:common` depends only on the
platform-neutral `kotlinx-coroutines-core`.

## Consequences
- `:domain`'s dependency on both modules is now a legal JVM-to-JVM
  dependency, resolvable by Gradle.
- Every Android module that depends on `core:common`/`core:model`
  (basically everything else in the project) is unaffected — an
  Android Library or Application module has always been able to depend on
  a plain JVM library module; only the reverse direction was ever a
  problem.
- `core:common` no longer has an `android { namespace = ... }` block at
  all, since a JVM library module has no Android namespace concept — this
  is expected, not an oversight.
- Precedent set: any future `core:*` module should default to
  `spaceav.jvm.library` unless it genuinely needs an Android API
  (`Context`, `PackageManager`, Compose, Room's Android integration,
  etc.) — `core:security` and `core:permissions`, for example, correctly
  stay on the Android Library plugin because they use `Context` directly.
