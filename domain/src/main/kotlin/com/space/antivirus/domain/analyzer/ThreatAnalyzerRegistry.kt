package com.space.antivirus.domain.analyzer

import com.space.antivirus.core.model.ScanTarget

/**
 * Contract for discovering which analyzers exist and which apply to a
 * given target. Implementation (a later sprint) will most likely use
 * Hilt's Multibindings (@IntoSet) to collect every bound ThreatAnalyzer
 * automatically — but that's an implementation detail this contract
 * doesn't commit to, which is exactly why it's a contract and not a
 * concrete class here.
 */
interface ThreatAnalyzerRegistry {

    /** Every registered analyzer, regardless of target compatibility. */
    fun allAnalyzers(): List<ThreatAnalyzer>

    /** Only the analyzers capable of processing the given target — the
     *  method most orchestration code should actually call. */
    fun analyzersFor(target: ScanTarget): List<ThreatAnalyzer>
}
