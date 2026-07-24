package com.space.antivirus.core.analysisengine.analyzer

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
import com.space.antivirus.domain.analyzer.identifier
import java.util.UUID
import javax.inject.Inject

/**
 * This project's first production ThreatAnalyzer. Operates on
 * InstalledApplicationInfo.requestedPermissions (Sprint 014 — see ADR
 * 0027 for why that field didn't exist until this sprint needed it).
 *
 * DESIGN PRINCIPLE — conservative by construction, not just by intent:
 * this analyzer never flags a single "dangerous" permission on its own.
 * Individually, READ_SMS, BIND_DEVICE_ADMIN, and INTERNET are all
 * requested by large numbers of entirely legitimate apps — flagging any
 * one of them alone would produce overwhelming false positives and
 * actively damage user trust (Sprint 002.75 §17: "never exaggerate
 * risk"). Every rule here requires a specific COMBINATION of permissions
 * that is, together, a well-established heuristic signal used by
 * real-world mobile security tooling — not an invented pattern.
 *
 * Two rules, each independently documented and independently testable:
 *
 * 1. SMS interception pattern: (READ_SMS or RECEIVE_SMS) + INTERNET.
 *    Legitimate messaging/OTP-autofill apps are the main class of app
 *    that needs SMS access at all; combining that with network access is
 *    also how SMS-intercepting malware exfiltrates intercepted messages.
 *    This heuristic cannot distinguish the two — that's exactly why the
 *    resulting Detection is RiskLevel.ATTENTION ("worth a look"), not
 *    RiskLevel.ACTION_NEEDED, and why system apps are excluded entirely
 *    (see below).
 *
 * 2. Device-admin lock pattern: BIND_DEVICE_ADMIN + INTERNET. Device
 *    administrator privileges make an app resistant to uninstallation;
 *    combined with network access, this is a well-known pattern in
 *    ransomware and lock-screen malware. Legitimate device-admin apps
 *    (MDM/enterprise management tools) exist and would also match this
 *    pattern — the same ATTENTION-not-ACTION_NEEDED reasoning applies.
 *
 * System apps (InstalledApplicationInfo.isSystemApp) are excluded from
 * both rules entirely, not just scored lower — they're trusted by
 * definition in this threat model, and a false "malware" flag on a core
 * Android system component would be a severe, trust-destroying false
 * positive, exactly the failure mode this analyzer is designed to avoid.
 */
class SuspiciousPermissionPatternAnalyzer @Inject constructor() : ThreatAnalyzer {

    override val id: AnalyzerId = AnalyzerId("suspicious-permission-pattern")

    override val capabilities: Set<AnalyzerCapability> = setOf(AnalyzerCapability.APPLICATION_ANALYSIS)

    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
        val applicationTarget = target as? ScanTarget.ApplicationTarget
            ?: return AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "SuspiciousPermissionPatternAnalyzer only handles ScanTarget.ApplicationTarget, " +
                        "got ${target::class.simpleName}. ThreatAnalyzerRegistry should never route a " +
                        "non-APPLICATION_ANALYSIS target here — this is a defensive check, not the " +
                        "expected path.",
                ),
            )

        val app = applicationTarget.application
        val targetIdentifier = target.identifier

        if (app.isSystemApp) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val permissions = app.requestedPermissions.toSet()
        val detections = mutableListOf<Detection>()

        if (matchesSmsInterceptionPattern(permissions)) {
            detections += Detection(
                id = UUID.randomUUID().toString(),
                analyzerId = id,
                threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                evidenceDescription = "Requests SMS access (READ_SMS or RECEIVE_SMS) together with " +
                    "INTERNET access — a permission combination commonly associated with " +
                    "SMS-intercepting malware.",
                riskLevel = RiskLevel.ATTENTION,
            )
        }

        if (matchesDeviceAdminLockPattern(permissions)) {
            detections += Detection(
                id = UUID.randomUUID().toString(),
                analyzerId = id,
                threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                evidenceDescription = "Requests device administrator privileges (BIND_DEVICE_ADMIN) " +
                    "together with INTERNET access — a permission combination commonly associated " +
                    "with ransomware and removal-resistant lock-screen malware.",
                riskLevel = RiskLevel.ATTENTION,
            )
        }

        return AppResult.Success(
            if (detections.isEmpty()) {
                AnalysisOutcome.Clean(targetIdentifier)
            } else {
                AnalysisOutcome.Flagged(targetIdentifier, detections)
            },
        )
    }

    private fun matchesSmsInterceptionPattern(permissions: Set<String>): Boolean =
        SMS_PERMISSIONS.any { it in permissions } && INTERNET_PERMISSION in permissions

    private fun matchesDeviceAdminLockPattern(permissions: Set<String>): Boolean =
        DEVICE_ADMIN_PERMISSION in permissions && INTERNET_PERMISSION in permissions

    private companion object {
        const val INTERNET_PERMISSION = "android.permission.INTERNET"
        const val DEVICE_ADMIN_PERMISSION = "android.permission.BIND_DEVICE_ADMIN"
        val SMS_PERMISSIONS = setOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
        )
    }
}
