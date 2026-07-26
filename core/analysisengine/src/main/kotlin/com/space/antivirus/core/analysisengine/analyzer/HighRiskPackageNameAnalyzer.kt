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
 * Sprint 027 — same identity-impersonation family as
 * AppIdentityImpersonationAnalyzer (Sprint 015), a different signal
 * within it: that analyzer checks the app's DISPLAY LABEL against a
 * known-brand list; this one checks whether a NON-system app's PACKAGE
 * NAME itself impersonates Android's own reserved system namespaces
 * (com.android.*, android.*, com.google.android.* claimed by a non-
 * system app) — a well-known malware-naming pattern intended to look
 * trustworthy in a package list or permission-grant dialog.
 *
 * Deliberately a small, exact-prefix check, not substring/fuzzy matching
 * anywhere in the package name — the same false-positive discipline
 * every analyzer in this project applies. A package merely CONTAINING
 * "android" somewhere (e.g. "com.mycompany.androidutils") is common and
 * legitimate; only an exact PREFIX match against these specific reserved
 * namespaces is checked.
 *
 * The isSystemApp exclusion is the load-bearing check here, not just
 * precedent: without it, every genuine system app would trip this rule
 * immediately, since real system apps legitimately live under these
 * exact namespaces.
 */
class HighRiskPackageNameAnalyzer @Inject constructor() : ThreatAnalyzer {

    override val id: AnalyzerId = AnalyzerId("high-risk-package-name")

    override val capabilities: Set<AnalyzerCapability> = setOf(AnalyzerCapability.APPLICATION_ANALYSIS)

    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
        val applicationTarget = target as? ScanTarget.ApplicationTarget
            ?: return AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "HighRiskPackageNameAnalyzer only handles ScanTarget.ApplicationTarget, " +
                        "got ${target::class.simpleName}.",
                ),
            )

        val app = applicationTarget.application
        val targetIdentifier = target.identifier

        if (app.isSystemApp) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val matchedNamespace = RESERVED_SYSTEM_NAMESPACES.firstOrNull { app.packageName.startsWith(it) }
            ?: return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))

        val detection = Detection(
            id = UUID.randomUUID().toString(),
            analyzerId = id,
            threatType = ThreatType.POTENTIALLY_UNWANTED_APPLICATION,
            evidenceDescription = "Package name (${app.packageName}) uses the \"$matchedNamespace\" " +
                "namespace, normally reserved for genuine Android system components, but this app " +
                "is not a system app — a pattern commonly associated with malware intentionally " +
                "named to look trustworthy in a package list.",
            riskLevel = RiskLevel.ATTENTION,
            confidence = Confidence.HIGH,
        )

        return AppResult.Success(AnalysisOutcome.Flagged(targetIdentifier, listOf(detection)))
    }

    private companion object {
        val RESERVED_SYSTEM_NAMESPACES = listOf(
            "com.android.",
            "com.google.android.",
            "android.",
        )
    }
}
