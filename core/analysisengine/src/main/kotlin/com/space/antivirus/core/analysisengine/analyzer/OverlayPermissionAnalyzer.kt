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
 * Sprint 027 — same conservative-combination discipline as
 * SuspiciousPermissionPatternAnalyzer (Sprint 014): SYSTEM_ALERT_WINDOW
 * (draw-over-other-apps) is never flagged alone — it's requested by many
 * entirely legitimate apps (floating chat heads, screen dimmers,
 * accessibility tools). Combined with INTERNET, it matches a real,
 * well-documented pattern: overlay-based credential-harvesting malware
 * (fake login screens drawn over legitimate apps) needs network access
 * to exfiltrate whatever it captures — the combination is the actual
 * signal, neither permission is on its own.
 *
 * System apps excluded entirely, same reasoning as every prior analyzer.
 */
class OverlayPermissionAnalyzer @Inject constructor() : ThreatAnalyzer {

    override val id: AnalyzerId = AnalyzerId("overlay-permission-pattern")

    override val capabilities: Set<AnalyzerCapability> = setOf(AnalyzerCapability.APPLICATION_ANALYSIS)

    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
        val applicationTarget = target as? ScanTarget.ApplicationTarget
            ?: return AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "OverlayPermissionAnalyzer only handles ScanTarget.ApplicationTarget, " +
                        "got ${target::class.simpleName}.",
                ),
            )

        val app = applicationTarget.application
        val targetIdentifier = target.identifier

        if (app.isSystemApp) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val permissions = app.requestedPermissions.toSet()
        val matches = OVERLAY_PERMISSION in permissions && INTERNET_PERMISSION in permissions

        if (!matches) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val detection = Detection(
            id = UUID.randomUUID().toString(),
            analyzerId = id,
            threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            evidenceDescription = "Can draw over other apps (SYSTEM_ALERT_WINDOW) with INTERNET " +
                "access — a pattern used by overlay credential-harvesting apps.",
            riskLevel = RiskLevel.ATTENTION,
            confidence = Confidence.MODERATE,
        )

        return AppResult.Success(AnalysisOutcome.Flagged(targetIdentifier, listOf(detection)))
    }

    private companion object {
        const val OVERLAY_PERMISSION = "android.permission.SYSTEM_ALERT_WINDOW"
        const val INTERNET_PERMISSION = "android.permission.INTERNET"
    }
}
