# ADR 0027: The first real ThreatAnalyzer, and the InstalledApplicationInfo gap it exposed

**Status:** Accepted

## Context
Sprint 014's brief specified a permission-based heuristic analyzer "operating on the existing `InstalledApplicationInfo` model so that it relies only on data already collected by the project." Checked before writing any analyzer code: `InstalledApplicationInfo` (Sprint 004B) carries no permission data at all, and its enumerator doesn't request `PackageManager.GET_PERMISSIONS`. The model's own KDoc had said permission analysis belonged to "a later sprint's analyzers, operating on this shape" — but the shape itself never actually grew to carry that data before this sprint needed it.

This is a real gap in a factual premise the brief assumed, not a hypothetical. The brief's own escape clause covers "architectural issues requiring a change to the approved roadmap" — this doesn't qualify as that: it's a small, necessary, foreseeable extension entirely within Phase A's stated purpose (detection capability), not a change to Phase A–F's structure. It was flagged directly before proceeding, then implemented rather than treated as a full-stop blocker, since the resolution was unambiguous.

## Decisions

### 1. Extend `InstalledApplicationInfo` with `requestedPermissions: List<String>`
A fourth breaking change to a shipped model in this project (after `Detection.analyzerId` — ADR 0015, `ScanStatistics.itemsInconclusive` — ADR 0017, `ScanStatistics.itemsTrusted` — ADR 0022), same category, same discipline: every one of the three existing construction call sites was found and fixed in this same patch, not left broken.

### 2. `InstalledApplicationEnumerator` requests `GET_PERMISSIONS`
`packageInfo.requestedPermissions` is nullable in the Android API (null, not empty, when a package declares no permissions at all) — normalized to an empty list at the enumeration boundary so nothing downstream ever has to handle a nullable permissions list.

### 3. Two permission-combination heuristics, never single-permission rules
`SuspiciousPermissionPatternAnalyzer` flags exactly two evidence-based combinations:
- **SMS interception:** (`READ_SMS` or `RECEIVE_SMS`) + `INTERNET`
- **Device-admin lock:** `BIND_DEVICE_ADMIN` + `INTERNET`

Neither permission is ever flagged alone. This is the concrete meaning of "conservative, minimize false positives" from the brief: individually, every permission involved is requested by large numbers of legitimate apps, and flagging any one of them alone would produce overwhelming noise. Both resulting `Detection`s use `RiskLevel.ATTENTION`, never `ACTION_NEEDED` — a heuristic that can't distinguish "messaging app" from "SMS trojan" shouldn't claim the certainty that tier implies (Sprint 002.75 §17).

### 4. System apps are excluded entirely, not scored lower
`InstalledApplicationInfo.isSystemApp` short-circuits both rules before they run. System apps are trusted by definition in this threat model; a false "malware" flag on a core Android component would be a severe, trust-destroying false positive — exactly the failure mode this analyzer exists to avoid, not an edge case to handle after the fact.

### 5. The analyzer stays in `core:analysisengine`, not its own module
ADR 0026 predicted future analyzers would live in "their own module." This one didn't need one — it's small, pure Kotlin, with no dependency beyond what `core:analysisengine` already has. `core:analysisengine`'s own KDoc already described itself as "the natural home for future analyzer implementations too." A genuinely larger or differently-dependent future analyzer can still get its own module later without requiring any change to this one, which is the property ADR 0026 actually cared about preserving — this isn't a contradiction of that decision, just a case where the predicted need for a separate module didn't materialize.

### 6. Real pipeline verification via a genuine JVM unit test, not fakes
`AnalysisPipelineIntegrationTest` (`core:analysisengine/src/test`) constructs `DefaultThreatAnalyzerRegistry`, `AnalyzerExecutor`, `AnalysisOutcomeAggregator`, `AnalyzeScanTargetUseCase`, and `SuspiciousPermissionPatternAnalyzer` together — all real production classes, no fakes, no mocks. This works as a plain JVM test (not instrumented) because none of these five classes touch Android or Room. This is the first time in this project that real multi-class integration has been verifiable without either a domain-layer fake or a full instrumented test — worth naming as a genuinely new, cheap verification tier this project now has access to for future analyzer work.

## Consequences
- Sprint 013's `AnalysisEngineBindingModuleTest` needed updating: its "the analyzer set is empty" assertion became false the moment this sprint's `@IntoSet` binding landed. Updated, not left as a silent, stale-but-passing regression — a test that still compiles and passes while asserting something no longer true would have been worse than no test at all.
- `RunScanRequestUseCase`, run against a device with this analyzer registered, now produces real `Flagged`/`Clean` outcomes for installed applications instead of universal `Inconclusive` — the concrete success criterion this sprint was measured against.
- The next `ThreatAnalyzer` (file-based, once Phase A continues) will need its own equivalent of this sprint's model-completeness check: does `FileMetadata` actually carry what a file-based heuristic needs, or does it have the same kind of gap `InstalledApplicationInfo` just had? Worth checking explicitly before assuming, not after.
