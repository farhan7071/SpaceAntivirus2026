package com.space.antivirus.core.ads

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether an ad may be shown right now — Sprint 044.
 *
 * Every ad in this app passes through here. Deliberately free of both
 * Android framework types and the Google SDK, so the one piece of logic
 * that can annoy a user or breach a policy is plain Kotlin and fully
 * testable on the JVM.
 *
 * The gate answers four questions in order, and any "no" is final:
 *
 * 1. Are ads enabled for this build at all?
 * 2. Has consent been resolved? (see [ConsentState])
 * 3. Is the app past its first-run grace period?
 * 4. Has enough time passed since the last interstitial?
 *
 * Banners skip 3 and 4 — they are ambient rather than interruptive, and
 * a frequency cap on a banner would mean an empty box where a banner
 * used to be, which looks broken.
 */
@Singleton
class AdsGate @Inject constructor(
    private val timeSource: AdTimeSource,
) {

    private var firstEligibleAtMillis: Long? = null
    private var lastInterstitialAtMillis: Long? = null

    /**
     * Called once the app knows its own build type. Starts the
     * first-run grace period clock.
     */
    fun onAppStarted() {
        if (firstEligibleAtMillis == null) {
            firstEligibleAtMillis = timeSource.currentTimeMillis()
        }
    }

    fun canShowBanner(consent: ConsentState, adsEnabled: Boolean): Boolean =
        adsEnabled && consent.allowsAds

    fun canShowInterstitial(consent: ConsentState, adsEnabled: Boolean): Boolean {
        if (!adsEnabled || !consent.allowsAds) return false

        val now = timeSource.currentTimeMillis()

        val startedAt = firstEligibleAtMillis ?: return false
        if (now - startedAt < AdsConfig.INTERSTITIAL_GRACE_PERIOD_MILLIS) return false

        val lastShown = lastInterstitialAtMillis ?: return true
        return now - lastShown >= AdsConfig.INTERSTITIAL_MIN_INTERVAL_MILLIS
    }

    /** Recorded only when an interstitial was genuinely displayed — a
     *  failed load must not consume the user's quiet period. */
    fun recordInterstitialShown() {
        lastInterstitialAtMillis = timeSource.currentTimeMillis()
    }
}

/**
 * Injected so the gate's timing rules are testable without sleeping.
 * The production binding is the system clock.
 */
interface AdTimeSource {
    fun currentTimeMillis(): Long
}

@Singleton
class SystemAdTimeSource @Inject constructor() : AdTimeSource {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

/**
 * Whether the user's consent position permits ads — Sprint 044.
 *
 * **This is a seam, not a consent implementation.** Google's UMP SDK is
 * not integrated in this sprint, and this type exists so that adding it
 * is a change in one place rather than a change everywhere an ad is
 * requested.
 *
 * The default is [UNKNOWN], which blocks ads. That direction is
 * deliberate and is the whole value of having the seam now: serving
 * personalised ads to a user in the EEA or UK without a Google-certified
 * consent platform breaches Google's EU User Consent Policy, and a
 * default that failed open would mean the app was in breach from the
 * moment real ad unit IDs were pasted in. Failing closed means the worst
 * case of shipping before UMP lands is no revenue, not a policy
 * violation.
 */
enum class ConsentState(val allowsAds: Boolean) {

    /** No consent decision available. Blocks ads. */
    UNKNOWN(allowsAds = false),

    /** The user is outside a consent-required region, or has consented. */
    GRANTED(allowsAds = true),

    /** The user declined. */
    DENIED(allowsAds = false),
}
