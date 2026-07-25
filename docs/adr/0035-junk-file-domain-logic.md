# ADR 0035: Junk-file domain logic — the last Phase C domain gap before Clean UI

**Status:** Accepted

## Context
Sprints 017–021 delivered Home, Onboarding, Security Center, Scan Execution, and Scan Results/History — five of the original Phase C roadmap's six named pieces, though not under their original sprint numbers or scope boundaries (the roadmap's "020 Security Center Results/History" ended up split into "Scan Execution" and "Scan Results/History" as the actual work was scoped). Before writing any code this sprint, current `main` was checked directly against the roadmap rather than assumed: `feature:clean` is still Sprint 003's literal placeholder text, and a repo-wide search confirmed zero junk-file domain concepts exist anywhere — no model, no repository, no use case. This is the one remaining gap before Phase C can close, and Clean UI cannot be honestly built without it existing first.

## Decisions

### 1. Reuse `EnumerationRepository.enumerateFiles`, no new repository
`EnumerationRepository` (Sprint 004B) already answers "what files exist under this scope" — the exact question junk detection needs answered first. No new repository, no new Hilt module, no new binding: `FindCleanableItemsUseCase` injects the same already-bound `EnumerationRepository` `RunScanRequestUseCase`'s target resolution already depends on. This is the direct, deliberate application of this sprint's "prefer existing project components over creating new abstractions" instruction.

### 2. Genuinely separate from `ThreatAnalyzer`/`RunScanRequestUseCase`
A cache file is not a security concern. Folding storage-reclamation into the threat-detection pipeline (`AnalysisOutcome`, `Detection`, `Threat`) would conflate two different domain concepts under one vocabulary that was deliberately built around evidence-based *threat* findings (ADR 0015's original reasoning for `Detection.analyzerId`). `CleanableItem`/`CleanableCategory` are new, standalone models — not variants of `Threat`/`ThreatType`.

### 3. Four conservative, evidence-based classification rules — `JunkFileClassifier`
Same discipline as `SuspiciousPermissionPatternAnalyzer`/`AppIdentityImpersonationAnalyzer` (Sprints 014/015): every rule is a well-established, low-ambiguity signal, not an invented heuristic.
- **`CACHE_FILE`**: a `/cache/` path segment — safe to clear by Android platform convention, the single least-ambiguous signal available.
- **`TEMPORARY_FILE`**: a small, closed extension set (`tmp`, `temp`, `bak`, `old`).
- **`LOG_FILE`**: `.log` extension.
- **`LEFTOVER_INSTALLER`**: a `.apk` in a Downloads-like path, **and** unmodified for at least 24 hours. The age requirement exists specifically so a just-downloaded installer the user may be about to run isn't flagged — location and extension alone aren't a strong enough signal on their own, unlike the other three rules.

Directories are never classified — only individual files.

### 4. `nowEpochMillis` is an explicit parameter, never read internally
Same principle as every timestamp elsewhere in this project (`ScanSession.startedAtEpochMillis`, etc.): `JunkFileClassifier.classify()` takes `nowEpochMillis` explicitly rather than calling `System.currentTimeMillis()` itself. This keeps the age-based `LEFTOVER_INSTALLER` rule fully deterministic and testable — same inputs, including "now", always produce the same output — without needing to mock time.

### 5. Candidates only — no deletion capability yet
`CleanableItem` models something a user *might* want to remove, not an action taken. Nothing in this domain layer deletes a file. That's explicitly Clean UI's job, once this layer exists for it to act on — consistent with this sprint's own instruction not to begin Clean UI until the domain logic underneath it exists.

## Consequences
- `FindCleanableItemsUseCase` takes a `ScanScope` parameter (matching the existing `UseCase<Params, Result>` pattern) rather than a fixed default scope — a future Clean screen can let a user choose which area to scan (Internal Storage, Downloads, etc.) with no UseCase change needed.
- No new `AppError` cases were needed — `EnumerationRepository`'s existing failure mapping (`PermissionMissing`/`StorageUnavailable`/`InvalidScanConfiguration`) already covers every failure this UseCase can encounter, confirmed directly rather than assumed.
- This closes Phase C's last domain-layer gap. The only remaining Phase C UI work is Clean itself, now unblocked.
