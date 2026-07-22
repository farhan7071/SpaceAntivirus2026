# ADR 0012: Split the Compose convention plugin by module type (library vs. application)

**Status:** Accepted

## Context
The first real Gradle sync against this project failed with:

```
'com.android.library' and 'com.android.application' plugins cannot be
applied in the same project.
```

Root cause, confirmed against the actual source: `app/build.gradle.kts`
applied both `spaceav.android.application` (→ `com.android.application`)
and `spaceav.android.library.compose`. The latter's implementation
(`AndroidLibraryComposeConventionPlugin`) unconditionally applies
`spaceav.android.library` (→ `com.android.library`) as its first step —
appropriate for every other Compose-using module in this project
(`core:ui`, `core:designsystem`, every `feature:*` module via
`spaceav.android.feature`), all of which genuinely are library modules.
`:app` is the one Compose-using module that is not a library module, and
no application-flavored equivalent existed for it.

This wasn't caught in Sprint 003 because — as the Sprint 003 report itself
should have flagged more prominently — nothing had actually been compiled
yet. It's a real gap in the convention-plugin design (one Compose plugin
that silently assumed "library" was the only possible base), not a typo.

## Decision
Add a second, application-flavored Compose convention plugin,
`spaceav.android.application.compose`, implemented by
`AndroidApplicationComposeConventionPlugin`. It applies
`spaceav.android.application` (not `spaceav.android.library`) and then
configures `ApplicationExtension.buildFeatures.compose = true` plus the
same Compose BOM/UI/Material3/tooling dependencies as the library variant.
`app/build.gradle.kts` now applies this instead of
`spaceav.android.library.compose`.

The existing `spaceav.android.library.compose` plugin is unchanged and
still used by every module that actually is a library module.

## Consequences
- `:app` now has a clean, non-conflicting plugin chain:
  `com.android.application` + Compose + Hilt, no `com.android.library`
  anywhere in its graph.
- No other module was touched. `core:ui`, `core:designsystem`, and every
  `feature:*` module keep using `spaceav.android.library.compose` exactly
  as before.
- There is now a small, intentional duplication between
  `AndroidLibraryComposeConventionPlugin` and
  `AndroidApplicationComposeConventionPlugin` — both wire up the same
  Compose dependency list, once against `LibraryExtension` and once
  against `ApplicationExtension`, because AGP's `buildFeatures.compose`
  toggle lives on different, non-shared extension types depending on
  module type. This could be factored into a shared private helper
  function later if a third variant is ever needed; not worth the
  indirection for two call sites today.
- General lesson for this convention-plugin hierarchy going forward: any
  future "add capability X" convention plugin needs to ask which module
  types it applies to before assuming "library" is the universal base —
  `:app` is the one Android module in this project that is never a
  library, and every capability plugin needs an application-flavored path
  if `:app` might use it.
