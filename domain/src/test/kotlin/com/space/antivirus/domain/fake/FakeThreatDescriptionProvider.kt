package com.space.antivirus.domain.fake

import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.reporting.ThreatDescriptionProvider

/**
 * Local to :domain's own test source set, same reasoning as every other
 * Fake* in this package: :domain cannot depend on core:testing.
 */
class FakeThreatDescriptionProvider(
    private val title: String = "fake title",
    private val description: String = "fake description",
) : ThreatDescriptionProvider {
    var lastTitleRequestArgs: Pair<ThreatType, List<Detection>>? = null
        private set
    var lastDescriptionRequestArgs: Pair<ThreatType, List<Detection>>? = null
        private set

    override fun titleFor(threatType: ThreatType, detections: List<Detection>): String {
        lastTitleRequestArgs = threatType to detections
        return title
    }

    override fun descriptionFor(threatType: ThreatType, detections: List<Detection>): String {
        lastDescriptionRequestArgs = threatType to detections
        return description
    }
}
