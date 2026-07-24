package com.space.antivirus.core.analysisengine.di

import com.space.antivirus.core.analysisengine.analyzer.SuspiciousPermissionPatternAnalyzer
import com.space.antivirus.domain.analyzer.DefaultThreatAnalyzerRegistry
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
import com.space.antivirus.domain.analyzer.ThreatAnalyzerRegistry
import com.space.antivirus.domain.scoring.HighestSeverityRiskScorer
import com.space.antivirus.domain.scoring.RiskScorer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

/**
 * Closes the two real DI gaps found during the Sprint 012 status review
 * (ADR 0026): ThreatAnalyzerRegistry and RiskScorer were both defined and
 * even had concrete implementations (DefaultThreatAnalyzerRegistry,
 * HighestSeverityRiskScorer, both in :domain since Sprints 004C/006) but
 * neither was ever actually bound into the Hilt graph.
 *
 * `:domain` cannot host this module itself — it's a pure-Kotlin module
 * with no Hilt/KSP processing applied (ADR 0005/0011). This module's
 * only job is to be the Hilt-enabled home for wiring domain's pure-Kotlin
 * classes into the graph; the classes being bound live entirely in
 * :domain, unchanged.
 *
 * @Multibinds declares that Set<ThreatAnalyzer> is a valid binding even
 * with zero @IntoSet contributions — required because DefaultThreatAnalyzerRegistry's
 * constructor requests that Set, and without this declaration Dagger has
 * no way to know an EMPTY set is a legitimate value rather than a missing
 * binding.
 *
 * As of Sprint 014, the Set is no longer empty: SuspiciousPermissionPatternAnalyzer
 * (also in this module, since it's small pure logic with no reason to live
 * anywhere else) contributes via @Binds + @IntoSet below. ADR 0026
 * predicted future analyzers would live in "their own module" — this one
 * didn't need one, and stayed here instead; a genuinely larger or
 * differently-dependent future analyzer can still get its own module
 * later without requiring any change to this one, which is the property
 * that actually mattered.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalysisEngineBindingModule {

    @Multibinds
    abstract fun bindThreatAnalyzers(): Set<@JvmSuppressWildcards ThreatAnalyzer>

    @Binds
    abstract fun bindThreatAnalyzerRegistry(impl: DefaultThreatAnalyzerRegistry): ThreatAnalyzerRegistry

    @Binds
    abstract fun bindRiskScorer(impl: HighestSeverityRiskScorer): RiskScorer

    @Binds
    @IntoSet
    abstract fun bindSuspiciousPermissionPatternAnalyzer(
        impl: SuspiciousPermissionPatternAnalyzer,
    ): ThreatAnalyzer
}
