package com.space.antivirus.core.analysisengine.di

import com.space.antivirus.core.analysisengine.analyzer.AppIdentityImpersonationAnalyzer
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
 * Two analyzers registered as of Sprint 015, both here for the same
 * reason ADR 0027 gave for the first: small, pure Kotlin, no reason to
 * live anywhere else. Phase A's plug-in architecture is holding exactly
 * as designed — adding the second analyzer required zero changes to
 * ThreatAnalyzerRegistry, AnalyzeScanTargetUseCase, or AnalyzerExecutor,
 * only one new @Binds + @IntoSet line here.
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

    @Binds
    @IntoSet
    abstract fun bindAppIdentityImpersonationAnalyzer(
        impl: AppIdentityImpersonationAnalyzer,
    ): ThreatAnalyzer
}
