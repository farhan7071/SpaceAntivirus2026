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
import com.space.antivirus.core.model.identifier
import java.util.UUID
import javax.inject.Inject

/**
 * Sprint 027 — installerPackageName is null for apps installed via ADB,
 * sideloaded APKs, or restored in ways PackageManager can't attribute to
 * a known installer. Deliberately the most conservative analyzer in this
 * project so far, LOW confidence: an unknown installer is common and
 * entirely legitimate — alternative app stores (F-Droid, Amazon
 * Appstore, OEM stores), developers testing their own builds, and
 * enthusiasts sideloading APKs from trusted sources are all completely
 * normal reasons to see this, not just malware distribution. This
 * analyzer cannot distinguish those cases from a genuinely suspicious
 * sideload — it only knows installer provenance is unknown, which is
 * real but weak evidence on its own. INFO severity, LOW confidence:
 * informational context worth having on record, explicitly not framed
 * as a warning.
 *
 * System apps excluded entirely, same reasoning as every prior analyzer
 * (system apps are pre-installed by the OS itself, not through any app
 * store, so they'd trip this rule universally and meaninglessly
 * otherwise).
 */
class UnknownInstallerSourceAnalyzer @Inject constructor() : ThreatAnalyzer {

    override val id: AnalyzerId = AnalyzerId("unknown-installer-source")

    override val capabilities: Set<AnalyzerCapability> = setOf(AnalyzerCapability.APPLICATION_ANALYSIS)

    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
        val applicationTarget = target as? ScanTarget.ApplicationTarget
            ?: return AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "UnknownInstallerSourceAnalyzer only handles ScanTarget.ApplicationTarget, " +
                        "got ${target::class.simpleName}.",
                ),
            )

        val app = applicationTarget.application
        val targetIdentifier = target.identifier

        if (app.isSystemApp || app.installerPackageName != null) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val detection = Detection(
            id = UUID.randomUUID().toString(),
            analyzerId = id,
            threatType = ThreatType.SUSPICIOUS_APP_CONFIGURATION,
            evidenceDescription = "Installed from an unrecognized source. Common for sideloaded " +
                "APKs or alternative app stores.",
            riskLevel = RiskLevel.INFO,
            confidence = Confidence.LOW,
        )

        return AppResult.Success(AnalysisOutcome.Flagged(targetIdentifier, listOf(detection)))
    }
}
