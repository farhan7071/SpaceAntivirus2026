package com.space.antivirus.domain.fake

import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.domain.reporting.ThreatDescriptionProvider

/**
 * Local to :domain's own test source set, same reasoning as every other
 * Fake* in this package: :domain cannot depend on core:testing.
 */
class FakeThreatDescriptionProvider(
    private val title: String = "fake title",
    private val description: String = "fake description",
    private val recommendation: String = "fake recommendation",
    private val shortSummary: String = "fake short summary",
    private val category: String = "fake category",
    private val confidenceLevel: String = "fake confidence level",
) : ThreatDescriptionProvider {
    var lastTitleRequestArgs: Pair<ThreatType, List<Detection>>? = null
        private set
    var lastDescriptionRequestArgs: Pair<ThreatType, List<Detection>>? = null
        private set
    var lastRecommendationRequestArgs: Triple<ThreatType, List<Detection>, RiskLevel>? = null
        private set
    var lastShortSummaryRequestArg: List<Detection>? = null
        private set
    var lastCategoryRequestArg: ThreatType? = null
        private set
    var lastConfidenceLevelRequestArgs: Pair<RiskLevel, List<Detection>>? = null
        private set

    override fun titleFor(threatType: ThreatType, detections: List<Detection>): String {
        lastTitleRequestArgs = threatType to detections
        return title
    }

    override fun descriptionFor(threatType: ThreatType, detections: List<Detection>): String {
        lastDescriptionRequestArgs = threatType to detections
        return description
    }

    override fun recommendationFor(threatType: ThreatType, detections: List<Detection>, riskLevel: RiskLevel): String {
        lastRecommendationRequestArgs = Triple(threatType, detections, riskLevel)
        return recommendation
    }

    override fun shortSummaryFor(detections: List<Detection>): String {
        lastShortSummaryRequestArg = detections
        return shortSummary
    }

    override fun categoryFor(threatType: ThreatType): String {
        lastCategoryRequestArg = threatType
        return category
    }

    override fun confidenceLevelFor(riskLevel: RiskLevel, detections: List<Detection>): String {
        lastConfidenceLevelRequestArgs = riskLevel to detections
        return confidenceLevel
    }
}
