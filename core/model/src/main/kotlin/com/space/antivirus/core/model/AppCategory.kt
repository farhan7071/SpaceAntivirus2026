package com.space.antivirus.core.model

/**
 * Mirrors android.content.pm.ApplicationInfo's own CATEGORY_* constants
 * (a real, stable Android API since API 26 — this project's exact
 * minSdk, ADR 0003, so no API-level branching is needed anywhere this
 * is populated). A developer declares this in their manifest
 * (android:appCategory) when publishing to Play; it's not this project
 * inferring anything — it's reading a real, self-reported classification
 * the OS already exposes.
 *
 * Sprint 028: added specifically so an analyzer can recognize when a
 * permission combination is EXPECTED for the app's own declared
 * category (a video-calling app legitimately needing camera+microphone+
 * internet, Sprint 028's own worked example) rather than flagging it
 * with no awareness of context. See ADR 0042.
 *
 * UNDEFINED covers both ApplicationInfo.CATEGORY_UNDEFINED (the app
 * never declared one) and any category value this project doesn't map —
 * deliberately the safe default an analyzer can't use to suppress a
 * finding, unlike a real, positively-identified category.
 */
enum class AppCategory {
    UNDEFINED,
    GAME,
    AUDIO,
    VIDEO,
    IMAGE,
    SOCIAL,
    NEWS,
    MAPS,
    PRODUCTIVITY,
}
