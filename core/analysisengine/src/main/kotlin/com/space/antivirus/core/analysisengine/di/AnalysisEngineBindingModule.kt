package com.space.antivirus.core.analysisengine.di

import com.space.antivirus.core.analysisengine.analyzer.AppIdentityImpersonationAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.DebuggableApplicationAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.DeviceAdministratorAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.HighRiskPackageNameAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.OverlayPermissionAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.SuspiciousPermissionPatternAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.SurveillanceCombinationAnalyzer
import com.space.antivirus.core.analysisengine.analyzer.UnknownInstallerSourceAnalyzer
import com.space.antivirus.core.analysisengine.reporting.ProductionThreatDescriptionProvider
import com.space.antivirus.domain.analyzer.DefaultThreatAnalyzerRegistry
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
import com.space.antivirus.domain.analyzer.ThreatAnalyzerRegistry
import com.space.antivirus.domain.reporting.ThreatDescriptionProvider
import com.space.antivirus.domain.scoring.CumulativeRiskScorer
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
 * Eight analyzers registered as of Sprint 027, all here for the same
 * reason ADR 0027 gave for the first: small, pure Kotlin, no reason to
 * live anywhere else. Phase A's plug-in architecture is holding exactly
 * as designed — six more analyzers this sprint required zero changes to
 * ThreatAnalyzerRegistry, AnalyzeScanTargetUseCase, or AnalyzerExecutor,
 * only new @Binds + @IntoSet lines here.
 *
 * As of Sprint 016, ThreatDescriptionProvider is also bound here — the
 * LAST binding ADR 0026 explicitly left open, deliberately deferred past
 * Sprints 013/014/015 pending real, governed copy (ADR 0016, ADR 0029).
 * With this binding, BuildThreatUseCase and therefore RunScanRequestUseCase
 * are, for the first time, fully Hilt-constructible.
 *
 * Sprint 027: RiskScorer now binds to CumulativeRiskScorer, replacing
 * HighestSeverityRiskScorer — a real strategy swap (ADR 0041), not an
 * addition, made possible by RiskScorer being an interface exactly so
 * this substitution could happen without touching any caller.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalysisEngineBindingModule {

    @Multibinds
    abstract fun bindThreatAnalyzers(): Set<@JvmSuppressWildcards ThreatAnalyzer>

    @Binds
    abstract fun bindThreatAnalyzerRegistry(impl: DefaultThreatAnalyzerRegistry): ThreatAnalyzerRegistry

    @Binds
    abstract fun bindRiskScorer(impl: CumulativeRiskScorer): RiskScorer

    @Binds
    abstract fun bindThreatDescriptionProvider(impl: ProductionThreatDescriptionProvider): ThreatDescriptionProvider

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

    @Binds
    @IntoSet
    abstract fun bindOverlayPermissionAnalyzer(impl: OverlayPermissionAnalyzer): ThreatAnalyzer

    @Binds
    @IntoSet
    abstract fun bindSurveillanceCombinationAnalyzer(impl: SurveillanceCombinationAnalyzer): ThreatAnalyzer

    @Binds
    @IntoSet
    abstract fun bindDeviceAdministratorAnalyzer(impl: DeviceAdministratorAnalyzer): ThreatAnalyzer

    @Binds
    @IntoSet
    abstract fun bindHighRiskPackageNameAnalyzer(impl: HighRiskPackageNameAnalyzer): ThreatAnalyzer

    @Binds
    @IntoSet
    abstract fun bindDebuggableApplicationAnalyzer(impl: DebuggableApplicationAnalyzer): ThreatAnalyzer

    @Binds
    @IntoSet
    abstract fun bindUnknownInstallerSourceAnalyzer(impl: UnknownInstallerSourceAnalyzer): ThreatAnalyzer
}
