package com.space.antivirus.core.model

/**
 * The ONLY three severity tiers this product uses anywhere — product
 * (Sprint 002.5 §17), content (Sprint 002.75 §4), and now domain. This is
 * the single source of truth for that vocabulary; core:ui's Severity enum
 * (in a later, UI-layer sprint) should map onto this rather than
 * duplicating it. Deliberately not a numeric score — Sprint 002.75 §4 is
 * explicit that inflated/numeric risk scores invite alarm-fatigue and
 * aren't something this engine can defensibly back up.
 *
 * Declaration order is meaningful: it's ascending severity
 * (INFO < ATTENTION < ACTION_NEEDED). Sprint 004C's RiskScorer relies on
 * this via `.ordinal` comparison — if this enum is ever reordered, that
 * comparison silently breaks, so don't reorder these without checking
 * every `.ordinal`/`compareTo` usage first.
 */
enum class RiskLevel {
    INFO,
    ATTENTION,
    ACTION_NEEDED,
}
