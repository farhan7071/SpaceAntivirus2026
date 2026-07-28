package com.space.antivirus.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sprint 030 — a small, closed set of icons for a card's compact
 * "reason icons" row (goal: "Camera • Internet • SMS • Overlay").
 * Inferred from evidence text keywords, the same approach and the same
 * keywords ProductionThreatDescriptionProvider uses independently
 * (core:analysisengine) for recommendationFor/shortSummaryFor — a
 * deliberate, small duplication across layers rather than a shared
 * dependency, since core:ui has zero dependency on domain/core:model and
 * shouldn't gain one just for this. See ADR 0044.
 *
 * Deliberately a SMALL set, not one icon per analyzer. core:ui already
 * depends on compose-material-icons-extended (unlike feature modules,
 * which this project has kept restricted to Icons.Default.Warning
 * throughout, per ADR 0031's standing caution), but the exact extended
 * icon names below have not been verified against a real compiler in
 * this sandbox — each was chosen for being among the longest-standing,
 * most stable names in the Material icon set, and every evidence type
 * this project's eight analyzers can't confidently map falls back to
 * Icons.Default.Warning, the one icon Sprint 017 actually confirmed safe
 * in this exact project. If any of PhotoCamera/Mic/Wifi/Sms/Layers turns
 * out wrong, this is an isolated, one-line compatibility fix in this
 * file only — nothing else in this sprint depends on which exact icon
 * renders.
 */
enum class EvidenceIcon(val imageVector: ImageVector) {
    CAMERA(Icons.Filled.PhotoCamera),
    MICROPHONE(Icons.Filled.Mic),
    INTERNET(Icons.Filled.Wifi),
    SMS(Icons.Filled.Sms),
    OVERLAY(Icons.Filled.Layers),
    OTHER(Icons.Filled.Warning),
    ;

    companion object {
        /**
         * Keyword matching against a single Detection's evidence text —
         * called once per detection by whichever screen builds a card's
         * evidence-icon row, since a single Detection's evidence can
         * reasonably imply more than one icon (e.g. a surveillance
         * finding implies both CAMERA and MICROPHONE).
         */
        fun inferFrom(evidenceText: String): Set<EvidenceIcon> {
            val text = evidenceText.lowercase()
            val icons = buildSet {
                if ("camera" in text) add(CAMERA)
                if ("microphone" in text) add(MICROPHONE)
                if ("internet" in text) add(INTERNET)
                if ("sms" in text) add(SMS)
                if ("draw over other apps" in text) add(OVERLAY)
            }
            return icons.ifEmpty { setOf(OTHER) }
        }
    }
}
