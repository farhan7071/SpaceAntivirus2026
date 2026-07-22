# Space Antivirus & Security — 2026 Rebuild

Engineering foundation produced in Sprint 003, implementing the architecture
decided in **Sprint 002** and the visual/content systems from **Sprint 002.5**
and **Sprint 002.75**. This is a from-zero rebuild — see Sprint 002.5's
"Project Decision" section for why the old app's UI/navigation/layouts are
not preserved, only used as a functional reference (Sprint 001).

## ⚠️ Known Limitation — Read Before Opening in Android Studio

This project was written in a sandboxed environment **without network access
to `dl.google.com` (Google's Maven repository) or `repo1.maven.org` (Maven
Central)**. That means:

- **No `./gradlew build` has been (or could be) run against this code.**
  Every file was written and statically self-reviewed for correctness, but
  dependency resolution — the first thing Gradle does — was not possible here.
- Library versions in `gradle/libs.versions.toml` reflect "latest stable
  as of Sprint 003 planning" from training/general knowledge, not a live
  check against Maven at the time you build this. **Re-run a version check
  in Android Studio's "upgrade assistant" or manually against
  `maven.google.com` before your first real build**, especially for AGP,
  Kotlin, and Compose BOM, which move quickly.
- The Gradle wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`) could not be
  downloaded here either — Android Studio will regenerate it automatically
  the first time you open the project and sync, or run
  `gradle wrapper --gradle-version 8.10.2` manually if needed.

**What this means practically:** treat this as a complete, carefully
reasoned source tree ready for its *first* real build — not as something
already proven to compile. Sprint 003's "Self Verification" section below
documents what was checked (structure, syntax, dependency direction) and
explicitly flags what could not be (an actual successful Gradle build).

## Architecture at a Glance

```
app                     — thin composition root (Application, MainActivity, NavHost)
core/
  common                — AppResult/AppError, DispatcherProvider
  model                 — shared data models (empty in Sprint 003, by design)
  designsystem          — M3 theme: color, type, shape, spacing tokens
  ui                     — reusable Compose components (buttons, cards, dialogs, ...)
  data                   — DataStore-backed repositories/preferences
  database               — Room (zero entities yet, by design)
  network                — Retrofit/OkHttp foundation (no endpoints yet, by design)
  security               — EncryptedSharedPreferences, crypto/Play-Integrity abstractions
  permissions             — permission status + rationale + settings-deeplink framework
  testing                 — MainDispatcherRule, test doubles shared by every module's tests
domain                    — pure-Kotlin UseCase base classes (no Android dependency)
feature/
  onboarding, home, security, clean, history, settings, premium,
  notifications, realtime
                           — one module per Sprint 002.5 top-level/reachable screen,
                             each a placeholder screen + empty ViewModel in Sprint 003
benchmark                  — Macrobenchmark module (startup time, per Sprint 002 §10)
build-logic                — Gradle convention plugins (spaceav.android.*) shared by every module
```

See `docs/architecture.md` for the full reasoning and `docs/adr/` for every
individual engineering decision with its context and tradeoffs.

## Module Dependency Direction

```
feature:* ──▶ domain ──▶ core:common, core:model
feature:* ──▶ core:designsystem, core:ui, core:permissions
app ────────▶ every feature:*, every core:*
```

No `feature:*` module depends on another `feature:*` module (ADR 0004).
`:domain` depends on nothing Android (ADR 0005). This is enforced by
convention (which dependencies the `spaceav.android.feature` plugin wires
in) rather than a build-time lint check in Sprint 003 — see ADR 0004's
Consequences for the planned Sprint 004 follow-up.

## What Sprint 003 Deliberately Does NOT Contain

Per the sprint's strict rules, no business logic exists yet:
- No malware scanning, junk cleaning, history, or premium feature logic
- No entities in Room, no endpoints in the network layer
- No `<service>` declaration for real-time protection (see ADR 0010 — this
  is deliberate, not an oversight: declaring a foreground service without
  its real `foregroundServiceType` and Play Console justification ready
  would be exactly the "temporary shortcut" the sprint rules prohibit)

## Self Verification (Sprint 003)

What **was** checked, without a working Gradle/Maven connection:
- [x] Every module has a `build.gradle.kts` applying only the convention
      plugins appropriate to its role (verified by manual read-through of
      all ~20 files)
- [x] `settings.gradle.kts` includes every module referenced anywhere in
      the tree, and no module is declared twice
- [x] No `feature:*` module's `build.gradle.kts` or source references
      another `feature:*` module (grepped for cross-feature imports — none found)
- [x] Every Kotlin file's package declaration matches its directory path
- [x] The navigation graph (`SpaceAntivirusNavHost.kt`) references exactly
      the route constants each feature module actually exports — cross-checked
      by grep, not by compiling
- [x] Two backtick-related content corruption bugs were found (via this
      same review discipline) and fixed in ADR 0005 and ADR 0009 — see git
      history / the fix itself as a demonstration of why this checklist exists
- [ ] **NOT verified: an actual successful `./gradlew build`.** This requires
      Maven/Google-repo network access this sandbox does not have. This is
      the first thing to run once you open the project in Android Studio.
- [ ] **NOT verified: app launches on a device/emulator.** No Android
      runtime available in this sandbox.

## First Steps in Android Studio

1. Open the project root in Android Studio (Ladybug/2024.2+ or newer recommended for AGP 8.7/Kotlin 2.0).
2. Let Gradle sync — this is the first real dependency-resolution pass.
3. Fix any version mismatches Android Studio's sync surfaces (see the
   "Known Limitation" note above — this is expected, not a sign something
   here is fundamentally wrong).
4. Run `./gradlew detekt ktlintCheck` before your first commit.
5. Run the unit tests: `./gradlew test` (covers `core:common`, `:domain`,
   `feature:home`'s ViewModel test).
6. Run on a device/emulator and confirm the app launches to the Onboarding
   placeholder screen with the 4-tab bottom bar reachable once you navigate
   past it — this is Sprint 003's actual functional bar, per the sprint's
   success criteria.

## Further Reading

- `docs/architecture.md` — full module-by-module rationale
- `docs/coding-standards.md` — naming, error handling, testing conventions
- `docs/contributing.md` — contribution/PR expectations
- `docs/adr/` — every significant engineering decision, with context and tradeoffs
