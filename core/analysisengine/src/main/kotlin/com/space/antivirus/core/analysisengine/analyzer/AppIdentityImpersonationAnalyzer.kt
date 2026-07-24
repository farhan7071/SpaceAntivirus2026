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
 * This project's second production ThreatAnalyzer — deliberately
 * evaluates a different risk dimension than Sprint 014's
 * SuspiciousPermissionPatternAnalyzer. That one asks "does this app's
 * CAPABILITIES look suspicious" (permission combinations); this one asks
 * "is this app pretending to be something it isn't" (identity). Neither
 * heuristic can catch what the other catches — genuinely complementary
 * coverage, not two variations on the same signal.
 *
 * DESIGN PRINCIPLE — same conservative-by-construction discipline as
 * Sprint 014: this only checks a SMALL, curated list of extremely
 * well-known, stable app identities (not general brand-name
 * fuzzy-matching, which would be both harder to keep accurate and far
 * more prone to false positives). An app is flagged only when its
 * display label is an EXACT match to one of these well-known names AND
 * its package name does NOT match that brand's real, official package —
 * both conditions required, both conservative by design:
 * - Exact label match, not substring/fuzzy — avoids flagging apps that
 *   merely mention a brand name (e.g. "WhatsApp Backup Tool").
 * - A small, deliberately short list of brands this project is
 *   confident about the real package name for — wrong entries here
 *   would cause false positives on the GENUINE real app, which would be
 *   a far worse failure than under-covering less common brands.
 *
 * System apps are excluded entirely before the check runs, same
 * reasoning as Sprint 014: they're trusted by definition in this threat
 * model.
 *
 * ThreatType.POTENTIALLY_UNWANTED_APPLICATION (not SUSPICIOUS_PERMISSION_USAGE,
 * which Sprint 014's analyzer already owns) — impersonating a trusted
 * brand's identity is a deception concern, not a permission-usage one;
 * using a distinct ThreatType here is also what keeps this analyzer's
 * findings naturally distinguishable from Sprint 014's, rather than
 * looking like duplicate/overlapping evidence for the same concern.
 */
class AppIdentityImpersonationAnalyzer @Inject constructor() : ThreatAnalyzer {

    override val id: AnalyzerId = AnalyzerId("app-identity-impersonation")

    override val capabilities: Set<AnalyzerCapability> = setOf(AnalyzerCapability.APPLICATION_ANALYSIS)

    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
        val applicationTarget = target as? ScanTarget.ApplicationTarget
            ?: return AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "AppIdentityImpersonationAnalyzer only handles ScanTarget.ApplicationTarget, " +
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

        val expectedPackageName = KNOWN_BRAND_PACKAGE_NAMES[app.appLabel]
        val isImpersonating = expectedPackageName != null && expectedPackageName != app.packageName

        if (!isImpersonating) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val detection = Detection(
            id = UUID.randomUUID().toString(),
            analyzerId = id,
            threatType = ThreatType.POTENTIALLY_UNWANTED_APPLICATION,
            evidenceDescription = "App is labeled \"${app.appLabel}\", matching a well-known app name, " +
                "but its package identity (${app.packageName}) does not match that app's real, " +
                "official package (${expectedPackageName}) — a pattern commonly associated with " +
                "apps impersonating a trusted brand.",
            riskLevel = RiskLevel.ATTENTION,
        )

        return AppResult.Success(AnalysisOutcome.Flagged(targetIdentifier, listOf(detection)))
    }

    private companion object {
        // Deliberately short. Every entry here must be an app identity
        // this project is genuinely confident about the real, official
        // package name for — see the class KDoc for why an incorrect
        // entry would be worse than no entry at all.
        val KNOWN_BRAND_PACKAGE_NAMES: Map<String, String> = mapOf(
            "WhatsApp" to "com.whatsapp",
            "Instagram" to "com.instagram.android",
            "Facebook" to "com.facebook.katana",
            "Google Chrome" to "com.android.chrome",
            "Google Play Store" to "com.android.vending",
        )
    }
}
