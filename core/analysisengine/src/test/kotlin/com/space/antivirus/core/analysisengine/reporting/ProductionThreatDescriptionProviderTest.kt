package com.space.antivirus.core.analysisengine.reporting

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ThreatType
import org.junit.Test

class ProductionThreatDescriptionProviderTest {

    private val provider = ProductionThreatDescriptionProvider()

    private fun detection(
        evidenceDescription: String = "test evidence",
        threatType: ThreatType = ThreatType.UNKNOWN,
        riskLevel: RiskLevel = RiskLevel.ATTENTION,
        analyzerId: AnalyzerId = AnalyzerId("test-analyzer"),
        confidence: Confidence = Confidence.MODERATE,
    ) = Detection(
        id = "d1",
        analyzerId = analyzerId,
        threatType = threatType,
        evidenceDescription = evidenceDescription,
        riskLevel = riskLevel,
        confidence = confidence,
    )

    // --- title coverage: every ThreatType ---

    @Test
    fun `every ThreatType has a distinct, non-blank title`() {
        val titles = ThreatType.entries.map { provider.titleFor(it, listOf(detection())) }

        titles.forEach { assertThat(it).isNotEmpty() }
        assertThat(titles.toSet()).hasSize(ThreatType.entries.size)
    }

    @Test
    fun `titles never claim a verdict the underlying analyzer can't support`() {
        // Sprint 002.75 §17 ("never exaggerate risk"), per docs/content-style-guide.md.
        val words = listOf("virus", "infected", "dangerous", "confirmed")
        ThreatType.entries.forEach { threatType ->
            val title = provider.titleFor(threatType, listOf(detection())).lowercase()
            words.forEach { word -> assertThat(title).doesNotContain(word) }
        }
    }

    @Test
    fun `title for MALWARE`() {
        assertThat(provider.titleFor(ThreatType.MALWARE, listOf(detection())))
            .isEqualTo("Potential malware detected")
    }

    @Test
    fun `title for POTENTIALLY_UNWANTED_APPLICATION`() {
        assertThat(provider.titleFor(ThreatType.POTENTIALLY_UNWANTED_APPLICATION, listOf(detection())))
            .isEqualTo("Possible app impersonation")
    }

    @Test
    fun `title for SUSPICIOUS_PERMISSION_USAGE`() {
        assertThat(provider.titleFor(ThreatType.SUSPICIOUS_PERMISSION_USAGE, listOf(detection())))
            .isEqualTo("Unusual permission combination")
    }

    @Test
    fun `title for UNKNOWN`() {
        assertThat(provider.titleFor(ThreatType.UNKNOWN, listOf(detection())))
            .isEqualTo("Flagged for review")
    }

    // --- description coverage: every ThreatType, evidence always included ---

    @Test
    fun `every ThreatType produces a non-blank description that includes the evidence text`() {
        ThreatType.entries.forEach { threatType ->
            val description = provider.descriptionFor(
                threatType,
                listOf(detection(evidenceDescription = "distinctive evidence marker $threatType")),
            )
            assertThat(description).isNotEmpty()
            assertThat(description).contains("distinctive evidence marker $threatType")
        }
    }

    @Test
    fun `SUSPICIOUS_PERMISSION_USAGE description explains why and suggests reviewing, not demanding removal`() {
        val description = provider.descriptionFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            listOf(detection(evidenceDescription = "requests SMS and INTERNET access")),
        )

        assertThat(description).contains("requests SMS and INTERNET access")
        assertThat(description).contains("doesn't necessarily mean")
        // ATTENTION-tier findings suggest, never demand (content-style-guide.md).
        assertThat(description.lowercase()).doesNotContain("uninstall this app now")
    }

    @Test
    fun `description with multiple detections includes every detection's evidence, not just one`() {
        val detections = listOf(
            detection(evidenceDescription = "first piece of evidence"),
            detection(evidenceDescription = "second piece of evidence"),
        )

        val description = provider.descriptionFor(ThreatType.SUSPICIOUS_PERMISSION_USAGE, detections)

        assertThat(description).contains("first piece of evidence")
        assertThat(description).contains("second piece of evidence")
    }

    @Test
    fun `description shows evidence from a detection whose type differs from the driving threatType`() {
        // Mirrors BuildThreatUseCase's real behavior: threatType passed in
        // is only the highest-severity detection's category, but
        // `detections` is the full list — a detection of a DIFFERENT
        // threatType must still have its evidence shown (content-style-
        // guide.md's always-show-evidence rule doesn't carve out an
        // exception for the non-driving findings).
        val detections = listOf(
            detection(
                threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                evidenceDescription = "permission evidence",
            ),
            detection(
                threatType = ThreatType.POTENTIALLY_UNWANTED_APPLICATION,
                evidenceDescription = "impersonation evidence",
            ),
        )

        // threatType param reflects only the driving category, per
        // BuildThreatUseCase's own logic.
        val description = provider.descriptionFor(ThreatType.SUSPICIOUS_PERMISSION_USAGE, detections)

        assertThat(description).contains("permission evidence")
        assertThat(description).contains("impersonation evidence")
    }

    @Test
    fun `rejects an empty detections list rather than producing evidence-free copy`() {
        val exception = runCatching {
            provider.descriptionFor(ThreatType.UNKNOWN, emptyList())
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `descriptions are deterministic - the same input always produces the same output`() {
        val detections = listOf(detection(evidenceDescription = "consistent evidence"))

        val first = provider.descriptionFor(ThreatType.MALWARE, detections)
        val second = provider.descriptionFor(ThreatType.MALWARE, detections)

        assertThat(first).isEqualTo(second)
    }

    // --- recommendationFor: Sprint 030 contextual behavior ---

    @Test
    fun `ACTION_NEEDED always recommends reviewing immediately, regardless of evidence content`() {
        val detections = listOf(detection(evidenceDescription = "camera, microphone, and internet access"))

        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            detections,
            RiskLevel.ACTION_NEEDED,
        )

        assertThat(recommendation).isEqualTo("Review immediately.")
    }

    @Test
    fun `camera or microphone evidence gets a media-specific recommendation, not the generic default`() {
        val detections = listOf(detection(evidenceDescription = "camera, microphone, and internet access"))

        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            detections,
            RiskLevel.ATTENTION,
        )

        assertThat(recommendation).contains("calls or media")
    }

    @Test
    fun `overlay evidence gets an overlay-specific recommendation`() {
        val detections = listOf(detection(evidenceDescription = "can draw over other apps with internet access"))

        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            detections,
            RiskLevel.ATTENTION,
        )

        assertThat(recommendation).contains("overlay access")
    }

    @Test
    fun `sms evidence gets an sms-specific recommendation`() {
        val detections = listOf(detection(evidenceDescription = "sms access with internet access"))

        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            detections,
            RiskLevel.ATTENTION,
        )

        assertThat(recommendation).contains("SMS access")
    }

    @Test
    fun `evidence matching no known keyword falls back to the threatType default`() {
        val detections = listOf(detection(evidenceDescription = "built as debuggable"))

        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_APP_CONFIGURATION,
            detections,
            RiskLevel.INFO,
        )

        assertThat(recommendation).contains("development, testing, or alternative app stores")
    }

    @Test
    fun `two different findings produce two different recommendations - not identical text for every app`() {
        // Directly the sprint's own requirement: "avoid repeating
        // identical text for every app."
        val cameraRecommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            listOf(detection(evidenceDescription = "camera and microphone access")),
            RiskLevel.ATTENTION,
        )
        val overlayRecommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            listOf(detection(evidenceDescription = "can draw over other apps")),
            RiskLevel.ATTENTION,
        )

        assertThat(cameraRecommendation).isNotEqualTo(overlayRecommendation)
    }

    // --- recommendationFor: Sprint 032, broadened contextual wording ---

    @Test
    fun `camera-microphone recommendation names several plausible legitimate app types, not just one`() {
        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            listOf(detection(evidenceDescription = "camera and microphone access")),
            RiskLevel.ATTENTION,
        )

        assertThat(recommendation).contains("calls or media")
        assertThat(recommendation).contains("ride-sharing")
    }

    @Test
    fun `overlay recommendation names several plausible legitimate app types, not just one`() {
        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            listOf(detection(evidenceDescription = "can draw over other apps")),
            RiskLevel.ATTENTION,
        )

        assertThat(recommendation).contains("overlay access")
        assertThat(recommendation).contains("navigation")
    }

    @Test
    fun `sms recommendation names banking authentication as an expected use, not just messaging`() {
        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            listOf(detection(evidenceDescription = "sms access with internet access")),
            RiskLevel.ATTENTION,
        )

        assertThat(recommendation).contains("SMS access")
        assertThat(recommendation).contains("banking authentication")
    }

    @Test
    fun `broadened wording still combines correctly with the LOW-confidence legitimacy sentence`() {
        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            listOf(detection(evidenceDescription = "sms access with internet access", confidence = Confidence.LOW)),
            RiskLevel.ATTENTION,
        )

        assertThat(recommendation).contains("banking authentication")
        assertThat(recommendation).contains("more likely expected behavior")
    }

    // --- shortSummaryFor: Sprint 030 ---

    @Test
    fun `camera and microphone evidence gets the media-capture short summary`() {
        val detections = listOf(detection(evidenceDescription = "camera, microphone, and internet access"))

        assertThat(provider.shortSummaryFor(detections)).isEqualTo("Can record and transmit media.")
    }

    @Test
    fun `overlay evidence gets the overlay short summary`() {
        val detections = listOf(detection(evidenceDescription = "can draw over other apps"))

        assertThat(provider.shortSummaryFor(detections)).isEqualTo("Can display content over other apps.")
    }

    @Test
    fun `unmatched evidence falls back to the first detection's own evidence text, not a placeholder`() {
        val detections = listOf(detection(evidenceDescription = "a genuinely novel piece of evidence text"))

        assertThat(provider.shortSummaryFor(detections)).isEqualTo("a genuinely novel piece of evidence text")
    }

    @Test
    fun `rejects an empty detections list`() {
        val exception = runCatching { provider.shortSummaryFor(emptyList()) }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `short summaries are deterministic`() {
        val detections = listOf(detection(evidenceDescription = "sms access with internet access"))

        val first = provider.shortSummaryFor(detections)
        val second = provider.shortSummaryFor(detections)

        assertThat(first).isEqualTo(second)
    }

    // --- recommendationFor: Sprint 031, "why might this still be legitimate" (ADR 0045, goal #5) ---

    @Test
    fun `when every detection is LOW confidence, the recommendation explains why it might still be legitimate`() {
        val detections = listOf(
            detection(evidenceDescription = "sms access with internet access", confidence = Confidence.LOW),
        )

        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            detections,
            RiskLevel.ATTENTION,
        )

        assertThat(recommendation).contains("more likely expected behavior")
    }

    @Test
    fun `the base recommendation is still present, not replaced, when the legitimacy sentence is appended`() {
        val detections = listOf(
            detection(evidenceDescription = "camera and microphone access", confidence = Confidence.LOW),
        )

        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            detections,
            RiskLevel.ATTENTION,
        )

        assertThat(recommendation).contains("calls or media")
        assertThat(recommendation).contains("more likely expected behavior")
    }

    @Test
    fun `when even one detection is MODERATE or higher confidence, no legitimacy sentence is appended`() {
        val detections = listOf(
            detection(
                evidenceDescription = "sms access with internet access",
                confidence = Confidence.LOW,
                analyzerId = AnalyzerId("analyzer-a"),
            ),
            detection(
                evidenceDescription = "device administrator privileges",
                confidence = Confidence.MODERATE,
                analyzerId = AnalyzerId("analyzer-b"),
            ),
        )

        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            detections,
            RiskLevel.ATTENTION,
        )

        assertThat(recommendation).doesNotContain("more likely expected behavior")
    }

    @Test
    fun `ACTION_NEEDED never gets the legitimacy sentence, even with LOW-confidence detections`() {
        // Urgency takes precedence - see the method's own KDoc. In
        // practice CumulativeRiskScorer wouldn't produce ACTION_NEEDED
        // from only LOW-confidence detections, but this confirms the
        // ordering is enforced here too, not just relied upon upstream.
        val detections = listOf(detection(confidence = Confidence.LOW))

        val recommendation = provider.recommendationFor(
            ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            detections,
            RiskLevel.ACTION_NEEDED,
        )

        assertThat(recommendation).isEqualTo("Review immediately.")
    }

    // --- categoryFor: Sprint 033, Part 2's "Threat Category" report field ---

    @Test
    fun `every ThreatType has a real, non-blank category label`() {
        for (threatType in ThreatType.entries) {
            val category = provider.categoryFor(threatType)
            assertThat(category).isNotEmpty()
        }
    }

    @Test
    fun `SUSPICIOUS_PERMISSION_USAGE maps to a Permission Usage category`() {
        assertThat(provider.categoryFor(ThreatType.SUSPICIOUS_PERMISSION_USAGE)).isEqualTo("Permission Usage")
    }

    @Test
    fun `different threat types produce different category labels`() {
        val categories = ThreatType.entries.map { provider.categoryFor(it) }

        assertThat(categories.toSet()).hasSize(ThreatType.entries.size)
    }

    // --- confidenceLevelFor: Sprint 033, Part 3's four-tier confidence label ---

    @Test
    fun `ACTION_NEEDED riskLevel always yields Very High, regardless of detection confidence`() {
        val detections = listOf(detection(confidence = Confidence.LOW))

        val level = provider.confidenceLevelFor(RiskLevel.ACTION_NEEDED, detections)

        assertThat(level).isEqualTo("Very High")
    }

    @Test
    fun `a HIGH-confidence detection with non-escalated riskLevel yields High`() {
        val detections = listOf(detection(confidence = Confidence.HIGH))

        val level = provider.confidenceLevelFor(RiskLevel.ATTENTION, detections)

        assertThat(level).isEqualTo("High")
    }

    @Test
    fun `a MODERATE-confidence detection with no HIGH detection yields Medium`() {
        val detections = listOf(detection(confidence = Confidence.MODERATE))

        val level = provider.confidenceLevelFor(RiskLevel.ATTENTION, detections)

        assertThat(level).isEqualTo("Medium")
    }

    @Test
    fun `only LOW-confidence detections yield Low`() {
        val detections = listOf(
            detection(confidence = Confidence.LOW, analyzerId = AnalyzerId("analyzer-a")),
            detection(confidence = Confidence.LOW, analyzerId = AnalyzerId("analyzer-b")),
        )

        val level = provider.confidenceLevelFor(RiskLevel.ATTENTION, detections)

        assertThat(level).isEqualTo("Low")
    }

    @Test
    fun `one HIGH detection among several LOW ones still yields High - the strongest signal wins`() {
        val detections = listOf(
            detection(confidence = Confidence.LOW, analyzerId = AnalyzerId("analyzer-a")),
            detection(confidence = Confidence.HIGH, analyzerId = AnalyzerId("analyzer-b")),
        )

        val level = provider.confidenceLevelFor(RiskLevel.ATTENTION, detections)

        assertThat(level).isEqualTo("High")
    }

    @Test
    fun `Very High takes precedence over any individual detection confidence`() {
        // Two independent MODERATE+ detections escalating to ACTION_NEEDED
        // (CumulativeRiskScorer, ADR 0041) is a stronger statement than
        // either detection's own confidence value alone.
        val detections = listOf(
            detection(confidence = Confidence.MODERATE, analyzerId = AnalyzerId("analyzer-a")),
            detection(confidence = Confidence.MODERATE, analyzerId = AnalyzerId("analyzer-b")),
        )

        val level = provider.confidenceLevelFor(RiskLevel.ACTION_NEEDED, detections)

        assertThat(level).isEqualTo("Very High")
    }

    @Test
    fun `confidenceLevelFor rejects an empty detections list`() {
        val exception = runCatching { provider.confidenceLevelFor(RiskLevel.ATTENTION, emptyList()) }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
