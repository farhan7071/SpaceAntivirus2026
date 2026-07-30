package com.space.antivirus.core.analysisengine.analyzer

import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalysisOutcome
import com.space.antivirus.core.model.AnalyzerCapability
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.AppCategory
import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanTarget
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.core.model.identifier
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
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
 *
 * Sprint 031 (ADR 0045): confidence is no longer a flat MODERATE for
 * either rule — both now go through ConfidenceModulation, which lowers
 * confidence one tier when the app was installed from a known app store
 * or (for the SMS rule specifically) declares AppCategory.SOCIAL, the
 * category messaging apps most plausibly declare. The device-admin rule
 * has no comparably reliable "consistent category" — Android's own
 * category taxonomy has no MDM/enterprise-management category to check
 * against — so only installer trust applies there. RiskLevel itself is
 * unchanged; CumulativeRiskScorer's existing "MODERATE+ confidence
 * required to co-escalate" rule (unchanged, ADR 0041) is what actually
 * benefits from this — a downgraded LOW-confidence finding simply can't
 * contribute toward escalating a Threat to ACTION_NEEDED anymore.
 *
 * Sprint 033: AppCategory.VIDEO added alongside SOCIAL for the SMS
 * rule, after real-device testing found short-form video apps (SMS used
 * for account/OTP verification, the same reason messaging apps need it)
 * declaring VIDEO rather than SOCIAL reaching full, undowngraded
 * confidence. The device-admin rule's own reasoning is unaffected —
 * still no category applies there, for the same reason as before.
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
                evidenceDescription = "SMS access with INTERNET access — a pattern linked to " +
                    "SMS-intercepting malware.",
                riskLevel = RiskLevel.ATTENTION,
                confidence = ConfidenceModulation.modulate(
                    base = Confidence.MODERATE,
                    app = app,
                    categoryIsConsistent = app.category in CATEGORIES_CONSISTENT_WITH_SMS,
                ),
            )
        }

        if (matchesDeviceAdminLockPattern(permissions)) {
            detections += Detection(
                id = UUID.randomUUID().toString(),
                analyzerId = id,
                threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                evidenceDescription = "Requests device administrator privileges with INTERNET " +
                    "access — a pattern linked to ransomware and lock-screen malware.",
                riskLevel = RiskLevel.ATTENTION,
                confidence = ConfidenceModulation.modulate(
                    base = Confidence.MODERATE,
                    app = app,
                    categoryIsConsistent = false,
                ),
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
        val CATEGORIES_CONSISTENT_WITH_SMS = setOf(AppCategory.SOCIAL, AppCategory.VIDEO)
    }
}
