package com.space.antivirus.core.ads

/**
 * Every ad identifier and flag in this project, in one file — Sprint 044.
 *
 * One place so a release checklist has one thing to check, and so no ad
 * unit ID is ever typed into a feature module.
 *
 * **Sprint 047: these are production units.** Sprint 044 shipped Google's
 * test units as the default, on the reasoning that live IDs in a debug
 * build generate invalid traffic against your own account and are a
 * common route to an AdMob suspension.
 *
 * That risk is now handled by [adsEnabled] rather than by the ID itself:
 * ads are off entirely in debug builds, checked against the installed
 * package's real debuggable flag. A developer build therefore requests
 * nothing at all, which is stronger protection than requesting a test ad
 * would have been.
 *
 * Note that swapping these IDs does not by itself make ads appear. The
 * consent gate still blocks everything — see `ConsentState` and
 * `docs/ads.md`.
 */
object AdsConfig {

    /** Production banner unit (Sprint 047, 2.0 release configuration). */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-1134409723930786/5346205412"

    /** Production interstitial unit (Sprint 047). */
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1134409723930786/7159864434"

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
