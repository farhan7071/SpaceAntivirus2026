package com.space.antivirus.core.analysisengine.reporting

import com.space.antivirus.core.model.Detection
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
 */
class ProductionThreatDescriptionProvider @Inject constructor() : ThreatDescriptionProvider {

    override fun titleFor(threatType: ThreatType, detections: List<Detection>): String =
        when (threatType) {
            ThreatType.MALWARE -> "Potential malware detected"
            ThreatType.POTENTIALLY_UNWANTED_APPLICATION -> "Possible app impersonation"
            ThreatType.SUSPICIOUS_PERMISSION_USAGE -> "Unusual permission combination"
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

    private fun leadInFor(threatType: ThreatType): String = when (threatType) {
        ThreatType.MALWARE ->
            "This app was flagged as potential malware based on the following:"
        ThreatType.POTENTIALLY_UNWANTED_APPLICATION ->
            "This app may be impersonating a well-known app. Here's why:"
        ThreatType.SUSPICIOUS_PERMISSION_USAGE ->
            "This app requests a permission combination worth reviewing:"
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
        ThreatType.UNKNOWN ->
            "Consider reviewing this app's details and permissions to decide whether it looks as expected."
    }
}
