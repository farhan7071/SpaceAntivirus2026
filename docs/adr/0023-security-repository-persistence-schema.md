# ADR 0023: SecurityRepository's real persistence schema — entities and DAOs only

**Status:** Accepted

## Context
`SecurityRepository` has been contract-only since Sprint 004A — six sprints of building the entire scan orchestration pipeline (004B/004C/005/006/007/009) against `FakeSecurityRepository` alone, with `AppDatabase` still holding only Sprint 003's `PlaceholderEntity` (which existed solely because Room requires at least one `@Entity` to compile). ADR 0014 named the pattern to follow when this finally happened: its own module, Room-backed, `@Binds`-based binding.

Before starting, a real constraint was surfaced and discussed directly: this sandbox has no Gradle/Maven network access and no Android runtime, meaning Room's KSP-generated code — and any instrumented test — cannot be compiled or run here, unlike every previous sprint's pure-Kotlin `domain` work. Given that, the work was deliberately split: this sprint builds only the entities and DAOs (the smallest, most inspectable unit of real Room code), and the actual `SecurityRepositoryImpl` — the mapping layer between these entities and `domain`'s models — is deferred to Sprint 011, reviewed separately.

## Decisions

### 1. Normalized schema, not JSON blobs
Four entities: `ScanSessionEntity`, `ScanStatisticsEntity` (1:1 with a completed session), `ThreatEntity` (many per session), `DetectionEntity` (many per threat). `Detection` has its own meaningful fields (analyzer provenance per ADR 0015, its own risk level) — collapsing it into a serialized JSON column on `ThreatEntity` would hide that structure behind an opaque blob, exactly the kind of shortcut this project has consistently avoided (see ADR 0015's own reasoning for why `Detection` needed provenance in the first place). CASCADE delete is used throughout: deleting a session removes its statistics and threats; deleting a threat removes its detections.

### 2. Enums stored as plain strings, no TypeConverter
`ScanSessionState`, `ThreatType`, `RiskLevel`, etc. are stored via their `.name` value directly as `String` columns, not through a Room `TypeConverter`. Fewer moving parts, and it keeps the DAOs' `@Query` string literals (e.g. `WHERE state IN ('PENDING', 'RUNNING')`) directly readable against the domain enums' declared names — a deliberate simplicity choice for a sprint specifically about producing the smallest, most reviewable Room surface, not the most abstracted one.

### 3. `ScanProgress` is NOT persisted
Deliberately no `ScanProgressEntity`. Progress snapshots are inherently transient, updated potentially many times per second during a running scan, and Sprint 006/009's own progress-publishing design (ADR 0018) is already best-effort/non-durable by nature. Round-tripping every progress update through SQLite would add I/O overhead for a value with no requirement to survive process death. The real `SecurityRepositoryImpl` (Sprint 011) is expected to back `observeScanProgress`/`updateScanProgress` with an in-memory store (e.g. a `MutableStateFlow` map), not Room — a real architectural decision, not an oversight.

### 4. `TrustedItemEntity` is out of scope
This sprint is scoped specifically to `SecurityRepository`'s persistence needs (ADR 0014's long-named gap). `TrustedItemRepository` (Sprint 008/009) still has no Room backing either, but that's separate, later work — not folded in here.

### 5. Schema version 1 → 2, via `fallbackToDestructiveMigration()`, not a real Migration
This database has never shipped with actual persisted rows (it held only `PlaceholderEntity`, itself never populated). Writing a real `Migration` object for a schema that has no real data to preserve would be manufacturing complexity this pre-1.0 project doesn't need yet. `fallbackToDestructiveMigration()` is the honest, correct choice at this stage — explicitly flagged in `DataModule.kt`'s own comment to be revisited the moment real user data needs to survive a schema change.

### 6. DAOs stay at the raw entity level — no `@Relation`/`@Transaction` joins
Every DAO method returns/accepts entity types directly (`ScanSessionEntity`, `List<ThreatEntity>`, etc.), never an assembled domain model or a Room `@Relation`-composed POJO. Multi-level relational queries (session → statistics → threats → detections, assembled into one `ScanResult`) are exactly the kind of Room feature most prone to subtle mistakes (ambiguous key mismatches, missing `parentColumn`/`entityColumn` parameters) that are hard to catch without a real compiler — deferring that assembly to Sprint 011's repository-layer Kotlin code (multiple simple DAO calls, composed in code) is a deliberate risk-reduction choice, not a limitation of Room itself.

## Consequences
- `AppDatabase` version 2 now declares 4 real entities and exposes 4 DAOs; `PlaceholderEntity` was removed as dead code now that real entities exist.
- Tests for these DAOs are **instrumented** (`androidTest`), not JVM unit tests — this project has no Robolectric configured, and DAOs need a real SQLite environment. They'll run during physical-device verification, not `./gradlew test`. `core:database`'s `testInstrumentationRunner` was added (previously only `:app` had one) specifically to make this module's `androidTest` source set runnable at all.
- Sprint 011's `SecurityRepositoryImpl` has a concrete, tested (once verified on-device) foundation to map against — the actual mapping/assembly logic, and the `@Binds` wiring per ADR 0014's pattern, is that sprint's full scope.
