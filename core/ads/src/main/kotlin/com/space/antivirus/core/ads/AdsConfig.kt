package com.space.antivirus.core.ads

/**
 * Every ad identifier and flag in this project, in one file — Sprint 044.
 *
 * One place so a release checklist has one thing to check, and so no ad
 * unit ID is ever typed into a feature module.
 *
 * **The IDs below are Google's official test units, and that is the
 * correct default.** They are documented, permanently-available demo
 * units published by Google specifically for development; they serve
 * real test ads and never generate revenue or invalid traffic. Shipping
 * with them costs nothing but revenue. Shipping *live* units in a debug
 * build, by contrast, generates invalid traffic against your own
 * account and is the single most common way developers get an AdMob
 * account suspended — so the safe value is the default and the live
 * value is the deliberate act, not the other way round.
 *
 * Replace [BANNER_AD_UNIT_ID], [INTERSTITIAL_AD_UNIT_ID] and the
 * manifest's APPLICATION_ID with your real units before release. See
 * `docs/ads.md` for the full release checklist.
 */
object AdsConfig {

    /** Google's published test banner unit. */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

    /** Google's published test interstitial unit. */
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    /**
     * Master switch. Ads are off in debug builds by default: an
     * engineer's device repeatedly loading ads against a live account is
     * exactly the invalid-traffic pattern AdMob suspends accounts for,
     * and an ad frame in the middle of UI work is noise.
     *
     * Evaluated against the real installed package's debuggable flag
     * rather than a compile-time constant, so this holds for a debug
     * build installed on any device.
     */
    fun adsEnabled(isDebugBuild: Boolean): Boolean = !isDebugBuild

    /**
     * Minimum gap between two interstitials.
     *
     * Not a number picked for revenue. Google's own ad-placement
     * guidance treats back-to-back interstitials as a policy risk, and
     * this app's users are here because they are worried about their
     * phone — an app that interrupts twice in a minute reads as the
     * thing they were worried about.
     */
    const val INTERSTITIAL_MIN_INTERVAL_MILLIS = 3 * 60 * 1_000L

    /**
     * Interstitials suppressed for this long after first launch.
     *
     * A user's first session is when they decide whether this app is
     * trustworthy. An interstitial during it is the worst possible
     * first impression for a security product, and Play's own policy
     * treats ads shown before a user has seen any content as
     * disruptive.
     */
    const val INTERSTITIAL_GRACE_PERIOD_MILLIS = 10 * 60 * 1_000L
}
