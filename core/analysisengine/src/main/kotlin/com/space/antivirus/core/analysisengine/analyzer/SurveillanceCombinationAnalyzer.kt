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
 * Sprint 027 — CAMERA + RECORD_AUDIO + INTERNET together: the classic
 * "can see, can hear, can transmit" surveillance-app pattern. Deliberately
 * requires all three, not any two — CAMERA+INTERNET alone describes any
 * photo-sharing app; RECORD_AUDIO+INTERNET alone describes any voice
 * messaging app; only the combination of all three, on an app that isn't
 * an obvious camera/communication app by nature, is a genuinely narrow
 * signal. This analyzer can't tell what KIND of app it's looking at
 * (that would need more than permission data), so HIGH confidence isn't
 * justified even with all three present — a real camera or video-calling
 * app legitimately needs exactly this combination. MODERATE confidence,
 * ATTENTION severity: worth reviewing, not alarming.
 *
 * Sprint 028 fix (real-device report — "some permission combinations
 * should include contextual explanations indicating that they are
 * expected for certain categories of legitimate applications, for
 * example video conferencing or messaging apps"): apps that declare
 * ApplicationInfo.CATEGORY_VIDEO or CATEGORY_SOCIAL (a real, developer-
 * declared, Play Store–facing classification, not this project
 * inferring anything) are excluded entirely, not flagged with softened
 * wording. A video-calling app legitimately needing camera+microphone+
 * internet isn't a borderline case worth a gentler warning — it's not
 * suspicious at all, and the more honest choice is not flagging it,
 * rather than flagging it with a caveat. This is a suppression, not a
 * confidence downgrade — a category is either a real, positively-
 * identified fact (skip entirely) or it isn't (evaluate normally); there
 * is no middle state where category partially excuses the finding. See
 * ADR 0042. Unchanged in Sprint 031 — this reasoning still holds exactly
 * as written.
 *
 * Sprint 031 (ADR 0045): for apps that DON'T declare VIDEO/SOCIAL and so
 * still reach a real Detection (e.g. a ride-sharing app with in-trip
 * safety recording, which may not declare either category even though
 * the permission combination is entirely expected for what it does),
 * confidence now goes through ConfidenceModulation for installer trust —
 * category consistency doesn't apply a second time here, since VIDEO/
 * SOCIAL are already fully suppressed above; only installer trust is
 * left as a real, additional legitimacy signal for whatever remains.
 *
 * System apps excluded entirely, same reasoning as every prior analyzer.
 */
class SurveillanceCombinationAnalyzer @Inject constructor() : ThreatAnalyzer {

    override val id: AnalyzerId = AnalyzerId("surveillance-permission-combination")

    override val capabilities: Set<AnalyzerCapability> = setOf(AnalyzerCapability.APPLICATION_ANALYSIS)

    override suspend fun analyze(target: ScanTarget): AppResult<AnalysisOutcome> {
        val applicationTarget = target as? ScanTarget.ApplicationTarget
            ?: return AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "SurveillanceCombinationAnalyzer only handles ScanTarget.ApplicationTarget, " +
                        "got ${target::class.simpleName}.",
                ),
            )

        val app = applicationTarget.application
        val targetIdentifier = target.identifier

        if (app.isSystemApp || app.category in EXPECTED_CATEGORIES) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val permissions = app.requestedPermissions.toSet()
        val matches = REQUIRED_PERMISSIONS.all { it in permissions }

        if (!matches) {
            return AppResult.Success(AnalysisOutcome.Clean(targetIdentifier))
        }

        val detection = Detection(
            id = UUID.randomUUID().toString(),
            analyzerId = id,
            threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            evidenceDescription = "Camera, microphone, and internet access together — can record " +
                "and transmit media. Expected for camera or communication apps.",
            riskLevel = RiskLevel.ATTENTION,
            confidence = ConfidenceModulation.modulate(
                base = Confidence.MODERATE,
                app = app,
                categoryIsConsistent = false,
            ),
        )

        return AppResult.Success(AnalysisOutcome.Flagged(targetIdentifier, listOf(detection)))
    }

    private companion object {
        val REQUIRED_PERMISSIONS = setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
        )
        val EXPECTED_CATEGORIES = setOf(AppCategory.VIDEO, AppCategory.SOCIAL)
    }
}
