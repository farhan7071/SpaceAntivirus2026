package com.space.antivirus.core.ads

/**
 * The closed set of places this app may show an ad — Sprint 044.
 *
 * An enum rather than a free-form string for the same reason
 * `SettingsRowControl` is a closed set (Sprint 043A): adding a placement
 * should be a deliberate act reviewed on its merits, not a one-line
 * convenience. There is no `OTHER` and no constructor taking a name.
 *
 * **What is deliberately absent is the important part.** There is no
 * placement for onboarding, for an in-progress scan, for the Security
 * Center findings list, or for the Cleaner's deletion flow. Those are
 * the moments a user is either deciding whether to trust this app or
 * acting on something they have been told is a risk. Interrupting either
 * is both a Play policy risk and, more to the point, the behaviour that
 * makes people distrust security apps. Because the type is closed, those
 * placements cannot be added by accident — only by editing this file.
 */
enum class AdPlacement(val description: String) {

    /**
     * A banner on the Scan History list — a passive, scrollable,
     * non-critical screen the user reaches deliberately, and the only
     * screen in this app that genuinely fits a banner.
     */
    HISTORY_BANNER("Banner beneath the scan history list"),

    /**
     * An interstitial after a manual scan has finished AND the user has
     * acknowledged the result.
     *
     * The acknowledgement matters: showing this the instant a scan
     * completes would cover the result the user asked for, which is both
     * a policy problem and the single most user-hostile thing an
     * antivirus app can do. By the time this fires, the user has already
     * read the outcome and dismissed it, so the ad sits at a genuine
     * transition point rather than in front of the content.
     */
    SCAN_COMPLETE_INTERSTITIAL("Interstitial after a completed manual scan is acknowledged"),
}
