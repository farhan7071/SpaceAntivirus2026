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
import com.space.antivirus.core.model.identifier
import com.space.antivirus.domain.analyzer.ThreatAnalyzer
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
 * Sprint 028 fix (real-device report — "some applications generate
 * multiple repetitive findings"): this analyzer now ALSO excludes apps
 * that have INTERNET permission, not just the "system app" check it
 * always had. The original design let this fire independently even when
 * SuspiciousPermissionPatternAnalyzer's device-admin+INTERNET rule
 * ALSO fired on the same app — two separate findings about essentially
 * the same underlying fact (this app has device admin privileges),
 * described in different words. This analyzer's own reason to exist is
 * to catch device-admin apps that the COMBO rule can't see (no INTERNET
 * permission at all); once an app has both device-admin AND INTERNET,
 * the combo rule already covers it, more specifically and at higher
 * severity — this analyzer adding a second, overlapping INFO-level
 * finding on top would be redundant, not additional evidence.
 * AnalysisOutcomeAggregator's own dedup (Sprint 015) is exact-match only
 * by design and can't collapse two genuinely differently-worded
 * detections like these — the fix has to be at the analyzer level, not
 * the aggregator. See ADR 0042.
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

        val permissions = app.requestedPermissions.toSet()
        val hasDeviceAdmin = DEVICE_ADMIN_PERMISSION in permissions
        val hasInternet = INTERNET_PERMISSION in permissions

        if (!hasDeviceAdmin || hasInternet) {
            // Either no device-admin capability at all, or the app also
            // has INTERNET — in which case SuspiciousPermissionPatternAnalyzer's
            // device-admin+INTERNET combo rule already covers this app,
            // more specifically and at higher severity. Adding a second,
            // overlapping finding here would be redundant, not additional
            // evidence.
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val detection = Detection(
            id = UUID.randomUUID().toString(),
            analyzerId = id,
            threatType = ThreatType.SUSPICIOUS_APP_CONFIGURATION,
            evidenceDescription = "Holds device administrator privileges — harder to uninstall, " +
                "can enforce device-wide policies. Common for MDM and security apps.",
            riskLevel = RiskLevel.INFO,
            confidence = Confidence.HIGH,
        )

        return AppResult.Success(AnalysisOutcome.Flagged(targetIdentifier, listOf(detection)))
    }

    private companion object {
        const val DEVICE_ADMIN_PERMISSION = "android.permission.BIND_DEVICE_ADMIN"
        const val INTERNET_PERMISSION = "android.permission.INTERNET"
    }
}
