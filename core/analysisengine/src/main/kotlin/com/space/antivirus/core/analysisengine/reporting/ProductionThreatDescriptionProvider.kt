package com.space.antivirus.core.analysisengine.reporting

import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.reporting.ThreatDescriptionProvider
import javax.inject.Inject

/**
 * The real ThreatDescriptionProvider (ADR 0016, deferred until now).
 * Written against docs/content-style-guide.md — see that file's own
 * provenance note first: no Sprint 002.75 source document has ever been
 * committed to this repository, despite 30+ files citing specific
 * section numbers from it. That gap was found and flagged before any
 * copy was written (per this sprint's own "stop and report missing
 * domain information" instruction), then fixed by consolidating the
 * scattered, consistently-applied citations into a real, checkable
 * artifact this class is written against — not by inventing new rules,
 * and not by silently proceeding without anything to check against.
 *
 * Title is a short, static, category-level label — never the verdict.
 * Description always incorporates EVERY Detection's evidence, not just
 * ones matching the driving threatType passed in: BuildThreatUseCase
 * only passes the highest-severity Detection's threatType for framing,
 * but `detections` is the full list, and the content-style-guide's
 * always-show-evidence rule requires every piece of it to be visible,
 * not just whichever one happened to drive the headline category.
 *
 * Sprint 029: descriptionFor's long-form output is kept as-is — the
 * restructured report UI (Sprint 030) uses it as the "technical
 * explanation" shown only in a card's expanded state, not its primary
 * always-visible display.
 *
 * Sprint 030: recommendationFor and shortSummaryFor both inspect
 * detections' own evidenceDescription text for known keywords, rather
 * than switching on threatType alone — threatType is too coarse for
 * genuinely contextual copy (every permission-combination analyzer
 * shares SUSPICIOUS_PERMISSION_USAGE). This is a real, deliberate
 * design choice, not a shortcut: keyword matching against evidence text
 * this class already controls the exact wording of (every analyzer's
 * evidenceDescription was written and verified in Sprint 029) is safer
 * and lower-risk than adding new structured "evidence category" fields
 * to Detection, which would mean another Room migration and touching
 * all eight analyzers again for a UI-only concern. The same keyword
 * approach is used independently in core:ui for evidence icon selection
 * (ADR 0044) — a deliberate, small duplication across layers rather than
 * a shared dependency, since core:ui has no dependency on domain and
 * shouldn't gain one just for this.
 */
class ProductionThreatDescriptionProvider @Inject constructor() : ThreatDescriptionProvider {

    override fun titleFor(threatType: ThreatType, detections: List<Detection>): String =
        when (threatType) {
            ThreatType.MALWARE -> "Potential malware detected"
            ThreatType.POTENTIALLY_UNWANTED_APPLICATION -> "Possible app impersonation"
            ThreatType.SUSPICIOUS_PERMISSION_USAGE -> "Unusual permission combination"
            ThreatType.SUSPICIOUS_APP_CONFIGURATION -> "App configuration worth reviewing"
            ThreatType.UNKNOWN -> "Flagged for review"
        }

    override fun descriptionFor(threatType: ThreatType, detections: List<Detection>): String {
        require(detections.isNotEmpty()) {
            "ProductionThreatDescriptionProvider requires at least one Detection to describe " +
                "(content-style-guide.md's always-show-evidence rule has nothing to show otherwise)"
        }

        val evidence = detections.joinToString(separator = " ") { it.evidenceDescription }

        return "${leadInFor(threatType)} $evidence ${suggestedActionFor(threatType)}"
    }

    /**
     * Urgency takes precedence over evidence-specific framing: an
     * ACTION_NEEDED finding (CumulativeRiskScorer's escalation for two or
     * more independent, meaningful signals) always reads as urgent,
     * regardless of which specific evidence produced it.
     */
    override fun recommendationFor(
        threatType: ThreatType,
        detections: List<Detection>,
        riskLevel: RiskLevel,
    ): String {
        if (riskLevel == RiskLevel.ACTION_NEEDED) {
            return "Review immediately."
        }

        val evidence = combinedEvidenceLowercase(detections)
        return when {
            "camera" in evidence || "microphone" in evidence ->
                "Expected if you actively use this application for calls or media."
            "draw over other apps" in evidence ->
                "Verify this application is trusted before allowing overlay access."
            "sms" in evidence ->
                "Review why this app needs SMS access if you don't expect it to."
            else -> defaultRecommendationFor(threatType)
        }
    }

    /**
     * A single, compact sentence for a card's always-visible collapsed
     * state — distinct from descriptionFor's long-form prose (the
     * expanded "technical explanation") and recommendationFor's action-
     * oriented text. Falls back to the first Detection's own evidence
     * text (already short since Sprint 029) if nothing here matches a
     * known pattern — never a generic placeholder.
     */
    override fun shortSummaryFor(detections: List<Detection>): String {
        require(detections.isNotEmpty()) {
            "shortSummaryFor requires at least one Detection — same reasoning as descriptionFor."
        }

        val evidence = combinedEvidenceLowercase(detections)
        return when {
            "camera" in evidence && "microphone" in evidence -> "Can record and transmit media."
            "draw over other apps" in evidence -> "Can display content over other apps."
            "sms" in evidence -> "Can read and send text messages."
            "device administrator" in evidence -> "Has elevated control over this device."
            "debuggable" in evidence -> "Built in a way that allows a debugger to attach."
            "unrecognized source" in evidence -> "Installed from an unverified source."
            "namespace" in evidence -> "Uses a package name that may be misleading."
            "impersonating" in evidence -> "May be impersonating another app."
            else -> detections.first().evidenceDescription
        }
    }

    private fun combinedEvidenceLowercase(detections: List<Detection>): String =
        detections.joinToString(separator = " ") { it.evidenceDescription }.lowercase()

    private fun defaultRecommendationFor(threatType: ThreatType): String = when (threatType) {
        ThreatType.MALWARE ->
            "Research this app or remove it if you don't recognize or trust its source."
        ThreatType.POTENTIALLY_UNWANTED_APPLICATION ->
            "Verify this app came from the official listing for the brand it names."
        ThreatType.SUSPICIOUS_PERMISSION_USAGE ->
            "Often expected for the right kind of app. Review if unexpected."
        ThreatType.SUSPICIOUS_APP_CONFIGURATION ->
            "Often normal for development, testing, or alternative app stores."
        ThreatType.UNKNOWN ->
            "Review this app's details and permissions if it doesn't look as expected."
    }

    private fun leadInFor(threatType: ThreatType): String = when (threatType) {
        ThreatType.MALWARE ->
            "This app was flagged as potential malware based on the following:"
        ThreatType.POTENTIALLY_UNWANTED_APPLICATION ->
            "This app may be impersonating a well-known app. Here's why:"
        ThreatType.SUSPICIOUS_PERMISSION_USAGE ->
            "This app requests a permission combination worth reviewing:"
        ThreatType.SUSPICIOUS_APP_CONFIGURATION ->
            "Something about how this app is built or installed is worth reviewing:"
        ThreatType.UNKNOWN ->
            "This app was flagged for review based on the following:"
    }

    private fun suggestedActionFor(threatType: ThreatType): String = when (threatType) {
        ThreatType.MALWARE ->
            "Consider researching this app or removing it if you don't recognize or trust its source."
        ThreatType.POTENTIALLY_UNWANTED_APPLICATION ->
            "Consider verifying this app came from the official app store listing for the brand it names."
        ThreatType.SUSPICIOUS_PERMISSION_USAGE ->
            "This doesn't necessarily mean the app is harmful — many legitimate apps use similar " +
                "permissions. Consider reviewing whether these permissions make sense for what this " +
                "app does, and checking your device's app permission settings if you're unsure."
        ThreatType.SUSPICIOUS_APP_CONFIGURATION ->
            "These are often legitimate for development, testing, or alternative app stores — " +
                "consider whether this specific app's configuration makes sense given where and how " +
                "you got it."
        ThreatType.UNKNOWN ->
            "Consider reviewing this app's details and permissions to decide whether it looks as expected."
    }
}
