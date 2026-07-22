package com.space.antivirus.core.model

/**
 * The ONLY three severity tiers this product uses anywhere — product
 * (Sprint 002.5 §17), content (Sprint 002.75 §4), and now domain. This is
 * the single source of truth for that vocabulary; core:ui's Severity enum
 * (in a later, UI-layer sprint) should map onto this rather than
 * duplicating it. Deliberately not a numeric score — Sprint 002.75 §4 is
 * explicit that inflated/numeric risk scores invite alarm-fatigue and
 * aren't something this engine can defensibly back up.
 */
enum class RiskLevel {
    INFO,
    ATTENTION,
    ACTION_NEEDED,
}
