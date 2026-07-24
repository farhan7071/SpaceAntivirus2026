# ADR 0031: Production onboarding flow, and closing a real gap found in Sprint 017's reported fixes

**Status:** Accepted

## Context
Sprint 017's verification reported two "compile-only compatibility corrections": a missing `androidx.compose.runtime.getValue` import, and `Icons.Default.ErrorOutline` swapped for `Icons.Default.Warning` (not part of this project's baseline, non-Extended Material icon set). Before starting Sprint 018, a check of the actual pushed `main` branch found `HomeScreen.kt` — the file whose `by` delegate and icon usage actually needed both fixes — still had neither applied. The `getValue` import that was added landed in `SpaceAntivirusNavHost.kt` instead, a file Sprint 017's patch never touched and which already had its own unrelated, pre-existing `by` usage since Sprint 003.

A real compiler couldn't settle this definitively — Compose code needs the full androidx dependency graph, unavailable in this sandbox without network access to Maven. But `by` delegate imports are resolved per-file in Kotlin, not globally, so the reasoning doesn't depend on a compiler to be sound: `HomeScreen.kt` needed its own `getValue` import regardless of what any other file had. This was flagged directly rather than assumed away; both fixes were then applied separately, in commit `2218df3` ("Fix Compose compatibility in HomeScreen"), landing on `main` ahead of this sprint's own patch rather than inside it.

## Decisions

### 1. Zero new Material icon usage anywhere in onboarding
Given the exact mistake that just surfaced — assuming `ErrorOutline` was part of the baseline icon set when it wasn't — the safest way to guarantee zero repeat this sprint is to not introduce any new icon dependency at all. `OnboardingScreen` is entirely text-based. Nothing in this sprint's scope required icons, so this cost nothing.

### 2. Defensive Compose test imports
`assertExists()`/`assertDoesNotExist()` were imported explicitly in `OnboardingScreenTest`, even though Sprint 017's `HomeScreenTest` used `.assertExists()` without an explicit import and passed verification — genuine uncertainty about why that worked isn't worth resolving by guessing again. An unnecessary import costs a harmless unused-import lint warning at worst; a missing one costs a build failure. Asymmetric risk, so the safe default was taken.

### 3. Bare Kotlin `assert()` replaced with Truth's `assertThat().isTrue()`
Caught during self-review: an early draft used bare `assert(callbackWasInvoked)` for three callback-verification tests. Kotlin's built-in `assert()` only throws when JVM assertions are enabled (`-ea`), which Android instrumented tests don't guarantee — a silently-disabled assertion means the test always passes regardless of whether the callback actually fired. Fixed before commit, not discovered after.

### 4. `OnboardingViewModel` kept minimal and Compose-independent
No domain `UseCase` is injected — onboarding content is static (`OnboardingContent.kt`). Kept as a real `HiltViewModel` anyway, for two reasons: consistency with ADR 0030's established pattern (every feature screen gets a testable ViewModel), and because page-navigation bounds-checking (never advancing past the last page, never retreating before the first) is exactly the kind of logic that belongs outside Compose, however small it is here. `onGetStarted`/onboarding-complete is deliberately NOT modeled as ViewModel state — it's a one-time navigation event, passed as a caller-supplied callback from `OnboardingRoute`, wired in `SpaceAntivirusNavHost`. A ViewModel holding a `NavController` reference to drive its own navigation would be a real architecture smell this project hasn't introduced anywhere else.

### 5. Static content lives in its own file, owned by neither the ViewModel nor the Screen
`OnboardingContent.kt` holds `OnboardingPage`/`OnboardingPages` — both the ViewModel (for bounds-checking against `OnboardingPages.size`) and the Screen (for rendering) depend on it equally. Adding a future onboarding page means appending one entry to this one list; no other file needs to change — the "easily extensible for future pages" requirement this sprint named directly.

### 6. Onboarding copy is honest about the app's current real capability
The four pages describe exactly what this app does today: checking installed applications for permission patterns and identity mismatches (Sprints 014/015). They explicitly state what it does *not* do — scan files, messages, photos, or browsing activity — and make no claim about real-time monitoring, since that's Phase D and doesn't exist yet. Nothing here overpromises relative to the actual production pipeline.

## Consequences
- Wired into `SpaceAntivirusNavHost`: completing onboarding navigates to Home with `popUpTo(OnboardingNavigationRoute) { inclusive = true }`, so pressing back from Home doesn't return to onboarding — standard, correct onboarding-flow navigation.
- The NavHost's own class-level KDoc, stale since Sprint 003 ("every route renders only a placeholder"), was corrected while already touching the file — now accurately states which two routes are real.
- Any future feature screen following ADR 0030's pattern should also default to zero new icon usage unless a specific, already-verified-safe icon is needed — worth treating as a standing caution for this project until a real compiler is available to check against, not just this sprint's workaround.
