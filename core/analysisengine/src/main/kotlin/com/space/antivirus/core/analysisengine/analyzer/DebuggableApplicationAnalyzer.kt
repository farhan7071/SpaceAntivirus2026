package com.space.antivirus.core.analysisengine.analyzer

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
import com.space.antivirus.domain.analyzer.identifier
import java.util.UUID
import javax.inject.Inject

/**
 * Sprint 027 — a debuggable release build (ApplicationInfo.FLAG_DEBUGGABLE)
 * is a real security smell: it allows a debugger to attach to the
 * running process, inspect and modify memory, and read data the app
 * would otherwise protect — capability a finished, production app has no
 * legitimate reason to ship with. Deliberately INFO severity, not
 * ATTENTION: developer/test builds sideloaded during normal development
 * work (including this project's own debug builds) are legitimately
 * debuggable, so this is informational awareness on its own, not
 * something that should read as alarming without another corroborating
 * signal alongside it.
 *
 * System apps excluded entirely, same reasoning as every prior analyzer.
 */
class DebuggableApplicationAnalyzer @Inject constructor() : ThreatAnalyzer {

    override val id: AnalyzerId = AnalyzerId("debuggable-application")

    override val capabilities: Set<AnalyzerCapability> = setOf(AnalyzerCapability.APPLICATION_ANALYSIS)

    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
        val applicationTarget = target as? ScanTarget.ApplicationTarget
            ?: return AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "DebuggableApplicationAnalyzer only handles ScanTarget.ApplicationTarget, " +
                        "got ${target::class.simpleName}.",
                ),
            )

        val app = applicationTarget.application
        val targetIdentifier = target.identifier

        if (app.isSystemApp || !app.isDebuggable) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val detection = Detection(
            id = UUID.randomUUID().toString(),
            analyzerId = id,
            threatType = ThreatType.SUSPICIOUS_APP_CONFIGURATION,
            evidenceDescription = "Built as debuggable — a debugger can attach to this app while " +
                "it's running and inspect or modify its data. Normal for apps still in " +
                "development or testing; a finished app you downloaded normally shouldn't be " +
                "shipped this way.",
            riskLevel = RiskLevel.INFO,
            confidence = Confidence.HIGH,
        )

        return AppResult.Success(AnalysisOutcome.Flagged(targetIdentifier, listOf(detection)))
    }
}
