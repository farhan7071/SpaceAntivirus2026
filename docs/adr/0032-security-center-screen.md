# ADR 0032: Security Center — content allocation and cross-screen duplication choices

**Status:** Accepted

## Context
Sprint 019 introduces no genuinely new UI architecture pattern — it follows ADR 0030's stateful/stateless split, reactive `stateIn(WhileSubscribed)` state, and mockk-on-repository testing exactly as Home (017) and Onboarding (018) did. What's worth recording here are two content-allocation decisions and one recurring implementation detail this sprint's self-review caught again.

## Decisions

### 1. Security Center reuses `ObserveScanHistoryUseCase`, surfacing more detail than Home
Home shows a compact summary of the latest scan (clean/not-clean, threat count). Security Center surfaces the *full* `Threat` list from that same latest scan — real titles and descriptions, populated by Sprint 016's `ProductionThreatDescriptionProvider`. No new UseCase was needed; this is the same reactive Flow, mapped to more detail.

### 2. Trusted items are deliberately absent from Security Center
Home already shows a trusted-items count. Repeating it here wouldn't add security-specific value — Security Center's distinct purpose is threat detail, not a second Home. This was a real choice, not an oversight; the brief listed trusted items as "where appropriate," and it wasn't judged appropriate here.

### 3. `ProtectionStatus` is duplicated, not shared, across `feature:home` and `feature:security`
Same derivation logic (`UNKNOWN`/`PROTECTED`/`NEEDS_ATTENTION` from the latest `ScanResult`) now exists independently in both feature modules. Feature modules don't depend on each other in this project — only `:app` composes them — so sharing would mean either a cross-feature dependency (a real architectural violation) or a new shared module for a five-line `when` expression (premature). Deliberately left duplicated under a rule-of-three: if a third screen needs the same derivation, that's the point to extract it, not before.

### 4. `RiskLevel` → `Severity` mapping lives in the Screen, not the ViewModel
`RiskLevel`'s own KDoc (Sprint 004A) already says `core:ui`'s `Severity` enum should map onto it. `SecurityCenterViewModel` exposes `ThreatSummary.riskLevel: RiskLevel` (domain type) directly; the mapping to `Severity` (UI-toolkit type) happens only in `SecurityCenterScreen.kt`, keeping the ViewModel free of any `core:ui` import — the same UI-toolkit-agnostic-ViewModel discipline ADR 0030 established.

### 5. Same recurring bug caught again: `stateIn`'s `Loading` emission
Every `SecurityCenterViewModelTest` initially cast `awaitItem()` straight to `Loaded`, missing that `stateIn(initialValue = Loading)` always emits `Loading` first to any new collector, as a genuinely separate event — the exact bug ADR 0030 first documented for `HomeViewModelTest`. Caught and fixed in all 6 tests before commit. Worth naming plainly that this is now a *known, recurring* pattern this project's own tests keep tripping on, not a one-off — any future `stateIn`-based ViewModel test should check for this explicitly during self-review, not rediscover it each time.

### 6. Icon discipline continued
Only `Icons.Default.Warning` is used anywhere in this screen — the one icon confirmed genuinely baseline-safe since Sprint 017's verification. No new icon was introduced or guessed at.

## Consequences
- Security Center is now a real, if narrow, source of threat detail — the first screen where a user can actually read a specific finding's title and description, not just a count.
- The `ProtectionStatus`-duplication decision should be revisited the moment a third screen (e.g., a future dashboard or notification-detail screen) needs the same derivation.
