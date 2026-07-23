# ADR 0014: Repository interfaces are bound to implementations via Hilt @Binds, in a dedicated module per capability

**Status:** Accepted

## Context
Sprint 004A defined `SecurityRepository` as an interface with no
implementation — deferred deliberately. Sprint 004B is the first sprint to
actually implement a domain repository interface (`EnumerationRepository`),
which means it's also the first sprint that has to decide two things no
prior ADR settled: (1) how a `domain` interface gets bound to its concrete
implementation in the Hilt graph, and (2) where that implementation lives.

## Decision
1. **Binding mechanism:** `@Binds` on an abstract Hilt `@Module`
   (`EnumerationBindingModule`), not `@Provides` on an object. `@Binds` is
   the idiomatic Hilt pattern specifically for "this interface has exactly
   one implementation" — it's more concise than a `@Provides` function
   that would just call the implementation's constructor, and it makes the
   interface-to-implementation relationship explicit in a way a `@Provides`
   function's body doesn't. `@Provides` remains correct for the cases this
   project already uses it for (`NetworkModule`, `DataModule`,
   `DispatcherModule`) — constructing something Hilt can't build via
   `@Inject` alone (a `Retrofit` instance, a `RoomDatabase`, a raw
   `CoroutineDispatcher`).
2. **Module placement:** each repository implementation gets its own
   dedicated Android module (`core:enumeration` for
   `EnumerationRepository`), rather than accumulating unrelated
   repository implementations inside `core:data`. `core:data` remains the
   home for cross-cutting data infrastructure (`DataStore`, the `Room`
   database instance itself, dispatcher bindings) that multiple
   repositories might share — not a dumping ground for every repository
   implementation regardless of what capability it represents.

## Consequences
- When `SecurityRepository` gets a real implementation in a later sprint,
  it should follow the same pattern: its own module (plausibly
  `core:securityengine` or similar, matching the naming style of
  `core:enumeration`) with an `@Binds`-based binding module, not folded
  into `core:data` or `core:enumeration`.
- `core:enumeration` depends on `:domain` (to see the interface it
  implements) in addition to `core:common`/`core:model` — this is the
  same dependency shape `core:data` already has toward `:domain`-adjacent
  types, so it's consistent with the existing graph, not a new kind of
  edge.
- `:app` must depend on every module containing a Hilt `@Module`
  (`core:data`, now also `core:enumeration`) for Hilt's component
  generation to discover it — this was already true for `core:data` since
  Sprint 003 and is not a new requirement, just a new module needing the
  same treatment.
- Splitting Android-dependent code (`ScanScopePathResolver`,
  `InstalledApplicationEnumerator`) from Android-free code
  (`FileTreeWalker`) within the same module — rather than one undifferentiated
  implementation class — is the same discipline `:domain` itself follows
  at the module level (ADR 0005), applied one level down. Worth stating as
  a general principle for future `core:*` implementation modules: isolate
  the Android-dependent surface to the smallest possible set of classes,
  even within a module that's necessarily Android-dependent overall.
