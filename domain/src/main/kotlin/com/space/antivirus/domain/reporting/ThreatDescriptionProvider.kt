package com.space.antivirus.domain.reporting

import com.space.antivirus.core.model.Detection
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
 */
interface ThreatDescriptionProvider {
    fun titleFor(threatType: ThreatType, detections: List<Detection>): String
    fun descriptionFor(threatType: ThreatType, detections: List<Detection>): String
}
