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
 * Sprint 027 — same identity-impersonation family as
 * AppIdentityImpersonationAnalyzer (Sprint 015), a different signal
 * within it: that analyzer checks the app's DISPLAY LABEL against a
 * known-brand list; this one checks whether a NON-system app's PACKAGE
 * NAME itself impersonates Android's own reserved system namespaces
 * (com.android.*, android.* claimed by a non-system app) — a well-known
 * malware-naming pattern intended to look trustworthy in a package list
 * or permission-grant dialog.
 *
 * Deliberately a small, exact-prefix check, not substring/fuzzy matching
 * anywhere in the package name — the same false-positive discipline
 * every analyzer in this project applies. A package merely CONTAINING
 * "android" somewhere (e.g. "com.mycompany.androidutils") is common and
 * legitimate; only an exact PREFIX match against these specific reserved
 * namespaces is checked.
 *
 * Sprint 028 fix (real-device false-positive report): "com.google.android."
 * was removed from the reserved namespace list. It was a genuine mistake
 * in Sprint 027's original design — that namespace is NOT exclusively
 * reserved for pre-installed system components the way "com.android."
 * and "android." are. Many entirely legitimate, Play-Store-distributed
 * Google apps use it (Gmail as com.google.android.gm, YouTube as
 * com.google.android.youtube, Maps as com.google.android.apps.maps), and
 * critically, isSystemApp only reflects whether an APK currently lives
 * in the read-only system partition — a Google app UPDATED via the Play
 * Store after first boot commonly becomes isSystemApp=false while still
 * being completely genuine. The isSystemApp exclusion this analyzer
 * already had could not protect against that case, since the exact apps
 * likely to trip this rule were exactly the ones no longer flagged as
 * system apps. "com.android." and "android." don't have this problem —
 * Google's own Play Store app-signing policies do not permit ordinary
 * third-party or even Google-published Play Store apps to use those
 * specific namespaces, making them a genuinely reliable signal on their
 * own. See ADR 0042.
 *
 * The isSystemApp exclusion remains load-bearing for what's left: real
 * AOSP/system components legitimately living under com.android./android.
 * still need it.
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
            evidenceDescription = "Package (${app.packageName}) uses the \"$matchedNamespace\" " +
                "namespace reserved for system apps, but isn't one — a pattern used by malware to " +
                "look trustworthy.",
            riskLevel = RiskLevel.ATTENTION,
            confidence = Confidence.HIGH,
        )

        return AppResult.Success(AnalysisOutcome.Flagged(targetIdentifier, listOf(detection)))
    }

    private companion object {
        val RESERVED_SYSTEM_NAMESPACES = listOf(
            "com.android.",
            "android.",
        )
    }
}
