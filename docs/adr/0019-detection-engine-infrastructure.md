# ADR 0019: Detection Engine Infrastructure — concurrency, fault isolation, and cancellation semantics

**Status:** Accepted

## Context
Sprint 006's objectives named six concerns for the "execution infrastructure" around `ThreatAnalyzer`: registration, scheduling, execution, metrics, cancellation, and fault isolation. Three of these required real behavior changes to already-shipped code (`AnalyzeScanTargetUseCase`, `RunScanRequestUseCase`), not just new additions, so each is documented here with its reasoning.

## Decisions

### 1. Registration: `DefaultThreatAnalyzerRegistry`
A real, production `ThreatAnalyzerRegistry` implementation, taking its analyzer set via plain constructor injection (`Set<ThreatAnalyzer>`). **Deliberately not wired to Hilt's `@IntoSet` multibinding yet** — that needs a Hilt-enabled Android module and a `@Multibinds` declaration for the current zero-analyzers case, which is real DI-graph work belonging to whichever future sprint adds the first actual `ThreatAnalyzer` implementation. Half-wiring a multibinding graph with nothing to bind would be exactly the kind of speculative, untestable setup this project has consistently avoided.

### 2. Scheduling & Execution: concurrent analyzer execution
`AnalyzeScanTargetUseCase` previously ran applicable analyzers sequentially in a `for` loop. It now launches each one concurrently (`coroutineScope { analyzers.map { async { executor.execute(...) } }.awaitAll() }`). This is real "scheduling" work in the sense the sprint asked for: multiple analyzers examining the same target no longer serialize behind each other. Safe specifically because every individual analyzer invocation is already exception-isolated by `AnalyzerExecutor` before it reaches the `async` block — no analyzer's crash can propagate up and cancel its sibling analyzers' coroutines.

### 3. Execution & Fault Isolation: `AnalyzerExecutor`
New class wrapping every single `ThreatAnalyzer.analyze()` call: catches unexpected exceptions (converting them to `AppResult.Failure(AppError.Unexpected(e))`) and records `AnalyzerExecutionMetrics` (duration, success). **The one critical correctness detail:** `CancellationException` is caught and immediately rethrown, never converted to a Failure — it extends `Exception` via `IllegalStateException`/`RuntimeException`, so a naive `catch (e: Exception)` would silently swallow structured-concurrency cancellation. This is the single most important line in this whole feature block to get right, and it's covered by a dedicated test (`AnalyzerExecutorTest`'s "CancellationException is rethrown, never swallowed").

**Behavior change in `AnalyzeScanTargetUseCase` (breaking, from Sprint 004C's original semantics):** previously, ANY single analyzer's `AppResult.Failure` aborted the whole multi-analyzer analysis for that target (fail-fast across analyzers). Now: if at least one analyzer succeeds, its outcome (aggregated with any other successes, via the existing `AnalysisOutcomeAggregator`) is used, and failed/crashed analyzers are simply excluded. Only if **every** applicable analyzer fails does the method return a `Failure` — using the first failure's error, since a total-failure state is a real operational problem, not the normal "nothing found" case, and deserves to look different from both `Inconclusive` and `Clean`. This is the concrete meaning of "fault isolation" for this sprint: one broken third-party engine no longer blinds the whole analysis for a target that other, working engines can still meaningfully assess.

### 4. Metrics: `AnalyzerExecutionMetrics`
`analyzerId`, `durationMillis`, `succeeded` — added to `core:model` since it's plausible future UI/diagnostics surface material, matching where `Detection`/`ScanProgress` already live. Not surfaced anywhere yet (no UI changes this sprint); `AnalyzerExecutor` produces it, nothing currently persists or observes it beyond tests.

### 5. Cancellation: structured concurrency, not a separate stop() method
`RunScanRequestUseCase` now checks `coroutineContext.ensureActive()` between targets. A caller cancels a running scan by cancelling the `Job`/coroutine the UseCase is running in — ordinary Kotlin structured concurrency — rather than through a bespoke cancellation API. On `CancellationException`, the session is transitioned to `CANCELLED` before rethrowing.

**Second critical correctness detail:** the cancellation cleanup write (`securityRepository.cancelScanSession(session.id)`) is wrapped in `withContext(NonCancellable) { ... }`. Without this, the cleanup call — itself a suspend function — would be cancelled immediately without executing, since it runs inside a coroutine that's already in a cancelling state. This is a well-known structured-concurrency pitfall; getting it wrong would mean cancelled scans silently stay stuck in `RUNNING` forever, exactly the bug this feature was meant to prevent.

## A note on process, not just architecture
While building this sprint's tests, a full re-read of `RunScanRequestUseCaseTest.kt` (not just a grep-based structure check) surfaced that Sprint 005 Feature Block 2's delivered patch had accidentally deleted an entire test's function signature during a prior edit — leaving orphaned statements sitting directly in the class body, which would not compile. That bug shipped in the actual pushed commit despite being reported as passing verification. It's fixed as part of this same patch (the file was already being touched for the cancellation test). Documenting this here because it's directly relevant to how much confidence to place in any single "tests pass" report, including this project's own history of them, and because the fix strategy going forward is explicit: full file reads after edits, not grep-count spot checks.

## Consequences
- Any future `ThreatAnalyzer` implementation automatically benefits from fault isolation and metrics the moment it's added to the registry's `Set<ThreatAnalyzer>` — no changes needed to `AnalyzeScanTargetUseCase` or `AnalyzerExecutor`.
- A future UI can safely offer a "Cancel scan" action once it has a reference to the `Job` the scan is running in — no new domain API needed beyond what already exists.
- `RunScanRequestUseCaseTest`'s cancellation test (`cancelling the scan transitions the session to CANCELLED`) needed a genuine suspension point to create a deterministic cancel-mid-flight window in virtual time — added `DelayingThreatAnalyzer`, a new test fake, for exactly this purpose.
