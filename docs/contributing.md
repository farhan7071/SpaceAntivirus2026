# Contributing

This is currently a solo-developer project (Farhan / Zx Force Soft), so this
document is written as a personal-workflow checklist rather than a
multi-contributor policy — update it if/when that changes.

## Before starting Sprint 004+ feature work
1. Re-read the relevant section of Sprint 002 (architecture), Sprint 002.5
   (UX), and Sprint 002.75 (content) for the screen you're building —
   these are frozen source-of-truth documents per Sprint 003's own context
   notes; don't reinterpret them mid-implementation.
2. Check `docs/adr/` for any decision that constrains what you're about to
   build (e.g. ADR 0004 before adding any cross-feature reference).
3. If your change doesn't fit an existing ADR's decision, write a new one
   before writing code — this project's own working pattern, established
   in Sprint 003.

## Making a change
1. Branch per feature/module (see `docs/coding-standards.md`).
2. Run `./gradlew detekt ktlintCheck test` locally before committing.
3. Keep `feature:*` modules isolated from each other (ADR 0004) — if you
   find yourself wanting to import from another feature module, that's a
   signal the shared logic belongs in `domain` or `core:*` instead.
4. Update the relevant doc (`architecture.md`, an ADR, or this file) in the
   same PR/commit as the code change it documents — not as a follow-up.

## Opening in Android Studio for the first time
See the README's "Known Limitation" section — this project has not yet had
a successful `./gradlew build` run against it, since it was authored in a
sandbox without Maven/Google-repo network access. Expect to resolve some
dependency-version friction on first sync; this is expected, not a sign of
a deeper problem with the architecture itself.
