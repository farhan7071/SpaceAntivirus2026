package com.space.antivirus.domain.analyzer

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.ScanTarget

/**
 * The plug-in contract every future detection engine implements —
 * signature-based, heuristic, AI/ML-based, cloud-reputation, behavioral,
 * or anything else. This interface is the entire point of Sprint 004C:
 * a new engine is a new class implementing this interface in its own
 * module, wired into the DI graph in whatever sprint builds it. Nothing
 * in `domain` — this interface included — should ever need to change to
 * accommodate a new analyzer type.
 *
 * Deliberately minimal: `analyze()` takes one ScanTarget and returns one
 * AnalysisOutcome. Batching, scheduling, and cross-analyzer coordination
 * are orchestration concerns that belong to a UseCase calling multiple
 * ThreatAnalyzers, not to this contract — keeping this interface small is
 * what keeps it stable as new analyzer types get added.
 */
interface ThreatAnalyzer {

    /** Stable identity for this analyzer, used as Detection provenance
     *  (see Detection.analyzerId, ADR 0015). Must be stable across app
     *  versions — changing it would orphan historical Detections'
     *  attribution. */
    val id: AnalyzerId

    /** What kinds of ScanTarget this analyzer can meaningfully process —
     *  used by ThreatAnalyzerRegistry to route targets only to analyzers
     *  that can actually handle them. */
    val capabilities: Set<AnalyzerCapability>

    /** Analyzes one target. AppResult.Failure is reserved for the
     *  analyzer failing to RUN at all (e.g. its own dependency —
     *  a signature database, a network call — is unavailable);
     *  AnalysisOutcome.Inconclusive (a Success case) is for the analyzer
     *  running fine but being unable to reach a determination about this
     *  specific target. Conflating those two would hide real operational
     *  failures behind an outcome that looks like a normal result. */
    suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome>
}
