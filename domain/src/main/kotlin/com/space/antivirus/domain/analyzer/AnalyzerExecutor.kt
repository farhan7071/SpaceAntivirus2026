package com.space.antivirus.domain.analyzer

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerExecutionMetrics
import com.space.antivirus.core.model.ScanTarget
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/** Pairs one analyzer's outcome with how it performed — what
 *  AnalyzerExecutor produces for every single ThreatAnalyzer invocation. */
data class AnalyzerExecutionOutcome(
    val result: AppResult<AnalysisOutcome>,
    val metrics: AnalyzerExecutionMetrics,
)

/**
 * The fault-isolation boundary around every individual ThreatAnalyzer
 * invocation. A third-party detection engine (signature, heuristic, AI,
 * cloud) is exactly the kind of dependency that can throw an unexpected,
 * un-typed exception rather than honoring the AppResult contract — this
 * class is what stops one broken analyzer from crashing the coroutine
 * running the whole multi-analyzer, multi-target scan.
 *
 * CRITICAL correctness detail: CancellationException must be caught and
 * immediately rethrown, NEVER converted to AppResult.Failure. It extends
 * Exception (via IllegalStateException/RuntimeException), so a naive
 * `catch (e: Exception)` would silently swallow structured-concurrency
 * cancellation — breaking a caller's ability to cancel a running scan
 * (see RunScanRequestUseCase's cancellation handling, ADR 0019). The
 * CancellationException catch clause must come first, and must rethrow.
 */
class AnalyzerExecutor @Inject constructor() {

    suspend fun execute(analyzer: ThreatAnalyzer, target: ScanTarget): AnalyzerExecutionOutcome {
        val startedAtMillis = System.currentTimeMillis()

        val result: AppResult<AnalysisOutcome> = try {
            analyzer.analyze(target)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (unexpected: Exception) {
            AppResult.Failure(AppError.Unexpected(unexpected))
        }

        val metrics = AnalyzerExecutionMetrics(
            analyzerId = analyzer.id,
            durationMillis = System.currentTimeMillis() - startedAtMillis,
            succeeded = result is AppResult.Success,
        )

        return AnalyzerExecutionOutcome(result, metrics)
    }
}
