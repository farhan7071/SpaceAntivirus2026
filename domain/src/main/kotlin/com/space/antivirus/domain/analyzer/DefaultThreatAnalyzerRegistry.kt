package com.space.antivirus.domain.analyzer

import com.space.antivirus.core.model.ScanTarget
import javax.inject.Inject

/**
 * The real ThreatAnalyzerRegistry implementation — same logic
 * FakeThreatAnalyzerRegistry already exercised in tests, now production
 * code. Takes its analyzer set via plain constructor injection rather
 * than committing to a specific DI collection-binding mechanism here.
 *
 * DEFERRED, on purpose: wiring this to Hilt's @IntoSet multibinding
 * (so every bound ThreatAnalyzer implementation is automatically
 * collected into the Set<ThreatAnalyzer> this class receives) needs a
 * Hilt-enabled Android module and a `@Multibinds` declaration for the
 * zero-analyzers case — that's real DI-graph work belonging to whichever
 * future sprint adds the first actual ThreatAnalyzer implementation, not
 * something to half-wire speculatively here with nothing to bind yet.
 * This class is ready to receive that Set the moment it exists.
 */
class DefaultThreatAnalyzerRegistry @Inject constructor(
    private val analyzers: Set<@JvmSuppressWildcards ThreatAnalyzer>,
) : ThreatAnalyzerRegistry {

    override fun allAnalyzers(): List<ThreatAnalyzer> = analyzers.toList()

    override fun analyzersFor(target: ScanTarget): List<ThreatAnalyzer> =
        analyzers.filter { target.requiredCapability in it.capabilities }
}
