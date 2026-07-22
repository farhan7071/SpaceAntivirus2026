# Coding Standards

Companion to `docs/architecture.md`. Covers Sprint 003 Task 15's
"coding standards" deliverable.

## Naming
- Package-by-feature: `com.space.antivirus.feature.<name>`, never
  `com.space.antivirus.activities` / `.fragments` (the old app's pattern
  per Sprint 001 — explicitly not carried forward).
- Composables: `PascalCase`, noun-first for stateless screens
  (`HomeScreen`), `<Name>Route` for the state-holding entry composable
  that wires a ViewModel (matches this codebase's existing
  `HomeRoute`/`HomeScreen` split — see `feature:home`).
- ViewModels: `<Feature>ViewModel`, always `@HiltViewModel`, always expose
  `StateFlow<XUiState>` never `MutableStateFlow` publicly.
- UseCases: `<Verb><Noun>UseCase`, e.g. `RunScanUseCase` (Sprint 004+ example).
- Route constants: `<Feature>NavigationRoute` (ADR 0009) — not `<Feature>Route`,
  which is reserved for the composable of the same conceptual name.

## Package organization
Package-by-feature inside each module (see Architecture Overview). Within a
feature module: `<Name>Route.kt`, `<Name>Screen.kt`, `<Name>ViewModel.kt` as
separate files even when small — keeps git history/diffs scoped per concern
as real UI logic is added in Sprint 004+.

## Error handling
Every Repository/UseCase boundary returns `AppResult<T>` (ADR 0007). Never
let a raw exception cross from `core:data`/`core:network`/`core:database`
into `domain` or a `feature:*` module — catch and map to `AppError` at the
Repository implementation, not further up the stack.

## Documentation
Every public `Repository`, `UseCase`, and `ViewModel` gets a one-paragraph
KDoc stating its responsibility (Sprint 002 §13). Private implementation
details don't require KDoc — avoid documentation-for-its-own-sake.

## Testing
- `domain` UseCases and `core:*` logic: plain JUnit + `Truth` assertions,
  no Android dependency, run via `./gradlew test`.
- ViewModels: `MainDispatcherRule` (from `core:testing`) + `runTest`, fake
  Repositories/UseCases injected manually (no Hilt in unit tests).
- Compose UI: `createAndroidComposeRule`, used sparingly — reserved for the
  design-system components in `core:ui` and for cross-screen navigation
  smoke tests (see `app`'s `NavigationSmokeTest`), not full-screen snapshot
  tests everywhere (Sprint 002 §13).
- Fakes over mocks where reasonable (a hand-written `FakeXRepository` in
  `core:testing` or the consuming module's test source set) — MockK is
  available (`libs.mockk`) for cases where a fake would be excessive
  boilerplate.

## Logging
Timber (to be added when the first log statement is actually needed —
not included in Sprint 003's dependency graph since there's nothing to
log yet). Release builds must strip verbose/debug logs. Never log a file
path, package name, or any value described as sensitive in
Sprint 002.75 §16, even at debug level.

## Git workflow
Feature branches per module/feature, matching Sprint 002 §12's phased
roadmap — one phase, one set of branches, merged to a stable
`develop`/`release` model. (Exact branch-naming convention is a
preference, not an architectural decision — align with whatever
ToolsNova/TN-EOS already uses elsewhere for consistency across projects,
per Sprint 002 §13.)

## Code review
Any PR touching a `core:*` module or `build-logic` needs a deliberate
self-review pass against this document and the relevant ADR(s) before
merging — these changes ripple into every feature module. `feature:*`-only
PRs can be reviewed more lightly, since `ADR 0004`'s module isolation
means a feature module's changes can't affect another feature module by
construction.
