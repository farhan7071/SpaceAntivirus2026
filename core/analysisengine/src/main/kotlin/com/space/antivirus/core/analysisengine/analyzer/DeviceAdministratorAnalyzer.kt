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
 * Sprint 027 — deliberately distinct from SuspiciousPermissionPatternAnalyzer's
 * existing device-admin+INTERNET rule (Sprint 014), not a duplicate of it.
 * That rule flags the COMBINATION as a ransomware/lock-screen-malware
 * pattern, at ATTENTION severity. This analyzer flags device
 * administrator privileges STANDALONE — an app can request this
 * capability without matching that combination (e.g. no INTERNET
 * permission at all) and it's still worth a user's awareness, since
 * device-admin apps are inherently harder to uninstall and can enforce
 * device-wide policies (password rules, remote wipe, camera disable).
 * Deliberately INFO severity, not ATTENTION — legitimate MDM/enterprise
 * management and some security apps use this capability routinely; on
 * its own, without any other corroborating signal, this is informational
 * awareness ("here's an app with elevated device control, worth
 * knowing"), not a finding that should read as alarming.
 *
 * System apps excluded entirely, same reasoning as every prior analyzer.
 */
class DeviceAdministratorAnalyzer @Inject constructor() : ThreatAnalyzer {

    override val id: AnalyzerId = AnalyzerId("device-administrator-standalone")

    override val capabilities: Set<AnalyzerCapability> = setOf(AnalyzerCapability.APPLICATION_ANALYSIS)

    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
        val applicationTarget = target as? ScanTarget.ApplicationTarget
            ?: return AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "DeviceAdministratorAnalyzer only handles ScanTarget.ApplicationTarget, " +
                        "got ${target::class.simpleName}.",
                ),
            )

        val app = applicationTarget.application
        val targetIdentifier = target.identifier

        if (app.isSystemApp) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        if (DEVICE_ADMIN_PERMISSION !in app.requestedPermissions) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val detection = Detection(
            id = UUID.randomUUID().toString(),
            analyzerId = id,
            threatType = ThreatType.SUSPICIOUS_APP_CONFIGURATION,
            evidenceDescription = "Holds device administrator privileges (BIND_DEVICE_ADMIN) — this " +
                "gives the app elevated control over your device (enforcing password rules, remotely " +
                "wiping data, disabling the camera) and makes it harder to uninstall than a typical " +
                "app. Common for legitimate enterprise/MDM and some security apps; worth confirming " +
                "you recognize and trust this one.",
            riskLevel = RiskLevel.INFO,
            confidence = Confidence.HIGH,
        )

        return AppResult.Success(AnalysisOutcome.Flagged(targetIdentifier, listOf(detection)))
    }

    private companion object {
        const val DEVICE_ADMIN_PERMISSION = "android.permission.BIND_DEVICE_ADMIN"
    }
}
