package com.space.antivirus.core.analysisengine.reporting

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalyzerId
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
    ) = Detection(
        id = "d1",
        analyzerId = analyzerId,
        threatType = threatType,
        evidenceDescription = evidenceDescription,
        riskLevel = riskLevel,
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
}
