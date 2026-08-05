package com.space.antivirus.core.ads

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Sprint 044. The gate is the only part of the ads stack that can annoy
 * a user or breach a policy, so it is deliberately plain Kotlin and
 * fully covered here — no emulator, no SDK, no network.
 *
 * Several of these assert that ads are *refused*. Those are the ones
 * worth keeping: the failure mode of an ads integration is showing too
 * much, too early, to someone who never agreed to it.
 */
class AdsGateTest {

    private class FakeTimeSource(var now: Long = 0L) : AdTimeSource {
        override fun currentTimeMillis(): Long = now
    }

    private val time = FakeTimeSource()
    private val gate = AdsGate(time)

    private fun startedAndPastGracePeriod() {
        gate.onAppStarted()
        time.now += AdsConfig.INTERSTITIAL_GRACE_PERIOD_MILLIS
    }

    // -- Consent -------------------------------------------------------

    /**
     * The default. Serving ads to an EEA or UK user without a certified
     * consent platform breaches Google's EU User Consent Policy, so an
     * unresolved state must block rather than allow.
     */
    @Test
    fun `unknown consent blocks everything`() {
        startedAndPastGracePeriod()

        assertThat(gate.canShowBanner(ConsentState.UNKNOWN, adsEnabled = true)).isFalse()
        assertThat(gate.canShowInterstitial(ConsentState.UNKNOWN, adsEnabled = true)).isFalse()
    }

    @Test
    fun `denied consent blocks everything`() {
        startedAndPastGracePeriod()

        assertThat(gate.canShowBanner(ConsentState.DENIED, adsEnabled = true)).isFalse()
        assertThat(gate.canShowInterstitial(ConsentState.DENIED, adsEnabled = true)).isFalse()
    }

    // -- Build flag ----------------------------------------------------

    @Test
    fun `disabled ads block everything even with consent`() {
        startedAndPastGracePeriod()

        assertThat(gate.canShowBanner(ConsentState.GRANTED, adsEnabled = false)).isFalse()
        assertThat(gate.canShowInterstitial(ConsentState.GRANTED, adsEnabled = false)).isFalse()
    }

    @Test
    fun `ads are off in debug builds and on in release builds`() {
        assertThat(AdsConfig.adsEnabled(isDebugBuild = true)).isFalse()
        assertThat(AdsConfig.adsEnabled(isDebugBuild = false)).isTrue()
    }

    // -- First-run grace period ----------------------------------------

    /**
     * A user's first session is when they decide whether a security app
     * is trustworthy. An interstitial during it is the worst possible
     * first impression.
     */
    @Test
    fun `no interstitial during the first-run grace period`() {
        gate.onAppStarted()
        time.now += AdsConfig.INTERSTITIAL_GRACE_PERIOD_MILLIS - 1

        assertThat(gate.canShowInterstitial(ConsentState.GRANTED, adsEnabled = true)).isFalse()
    }

    @Test
    fun `interstitial allowed once the grace period has elapsed`() {
        startedAndPastGracePeriod()

        assertThat(gate.canShowInterstitial(ConsentState.GRANTED, adsEnabled = true)).isTrue()
    }

    /** Banners are ambient, not interruptive, so the grace period and
     *  the frequency cap do not apply to them. */
    @Test
    fun `banners are unaffected by the grace period`() {
        gate.onAppStarted()

        assertThat(gate.canShowBanner(ConsentState.GRANTED, adsEnabled = true)).isTrue()
    }

    /** Nothing may show before the app has reported that it started —
     *  the clock has not begun, so the grace period cannot have passed. */
    @Test
    fun `no interstitial before the app has started`() {
        assertThat(gate.canShowInterstitial(ConsentState.GRANTED, adsEnabled = true)).isFalse()
    }

    // -- Frequency cap -------------------------------------------------

    @Test
    fun `a second interstitial is refused inside the minimum interval`() {
        startedAndPastGracePeriod()
        gate.recordInterstitialShown()
        time.now += AdsConfig.INTERSTITIAL_MIN_INTERVAL_MILLIS - 1

        assertThat(gate.canShowInterstitial(ConsentState.GRANTED, adsEnabled = true)).isFalse()
    }

    @Test
    fun `a second interstitial is allowed once the interval has passed`() {
        startedAndPastGracePeriod()
        gate.recordInterstitialShown()
        time.now += AdsConfig.INTERSTITIAL_MIN_INTERVAL_MILLIS

        assertThat(gate.canShowInterstitial(ConsentState.GRANTED, adsEnabled = true)).isTrue()
    }

    /**
     * Only a genuinely displayed ad consumes the quiet period. A failed
     * load must not cost the user their next eligible moment.
     */
    @Test
    fun `an unshown interstitial does not consume the quiet period`() {
        startedAndPastGracePeriod()

        assertThat(gate.canShowInterstitial(ConsentState.GRANTED, adsEnabled = true)).isTrue()
        assertThat(gate.canShowInterstitial(ConsentState.GRANTED, adsEnabled = true)).isTrue()
    }

    // -- Placement set --------------------------------------------------

    /**
     * The placement enum is closed on purpose: onboarding, in-progress
     * scans, Security Center findings and the Cleaner's deletion flow
     * must not become ad slots by accident. This test fails the moment
     * someone adds one, which is the intended prompt to justify it.
     */
    @Test
    fun `only two placements exist`() {
        assertThat(AdPlacement.entries).hasSize(2)
        assertThat(AdPlacement.entries).containsExactly(
            AdPlacement.HISTORY_BANNER,
            AdPlacement.SCAN_COMPLETE_INTERSTITIAL,
        )
    }
}
