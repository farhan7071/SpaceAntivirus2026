package com.space.antivirus.domain.reporting

import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ThreatType

/**
 * Produces the user-facing title/description for a Threat. A contract,
 * not a default implementation, on purpose: real title/description copy
 * must follow Sprint 002.75's approved Vocabulary Dictionary (§4) and
 * Security Messaging Guide (§7), and go through that document's content-
 * governance review (§20) before it ships. Inventing plausible-sounding
 * English strings inside `domain` — even well-intentioned ones — would
 * bypass that review entirely, which is exactly the kind of unreviewed,
 * ad-hoc content Sprint 002.75 §20 exists to prevent.
 *
 * Whichever module implements this (a later sprint) owns getting that
 * copy reviewed against Sprint 002.75 before shipping it. `domain` only
 * defines the seam.
 *
 * `recommendationFor` was added in Sprint 029 as a short, actionable,
 * threatType-keyed recommendation, distinct from `descriptionFor`'s
 * longer prose. Sprint 030 extended its signature to also take
 * `detections` and `riskLevel` — threatType alone is too coarse for
 * genuinely contextual recommendations (every permission-combination
 * analyzer shares SUSPICIOUS_PERMISSION_USAGE, for instance); the real
 * evidence and severity already available at every call site let a
 * camera+microphone finding read differently from an overlay finding,
 * and an ACTION_NEEDED finding read more urgently than an INFO one,
 * without inventing any new persisted data. Deliberately NOT persisted
 * on Threat: it's derived entirely from fields Threat already carries
 * (threatType, riskLevel) plus detections it already has a full list of,
 * so recomputing it at display time needs no Room schema change.
 *
 * `shortSummaryFor` was added in Sprint 030 — a single, compact sentence
 * for a card's always-visible collapsed state, distinct from both
 * `descriptionFor`'s long-form prose (kept for the expanded "technical
 * explanation" section) and `recommendationFor`'s action-oriented text.
 * Same reasoning: derived from detections already available, not a new
 * persisted field.
 */
interface ThreatDescriptionProvider {
    fun titleFor(threatType: ThreatType, detections: List<Detection>): String
    fun descriptionFor(threatType: ThreatType, detections: List<Detection>): String
    fun recommendationFor(threatType: ThreatType, detections: List<Detection>, riskLevel: RiskLevel): String
    fun shortSummaryFor(detections: List<Detection>): String
}
