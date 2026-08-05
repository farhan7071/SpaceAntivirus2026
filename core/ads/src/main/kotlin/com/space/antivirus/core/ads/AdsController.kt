package com.space.antivirus.core.ads

import android.app.Activity

/**
 * The whole of this app's ad surface — Sprint 044.
 *
 * Four methods, and no Google SDK type in any signature. Everything
 * outside `core:ads` depends on this interface, so the SDK can be
 * upgraded, stubbed or removed without touching a feature module, and
 * tests can substitute a fake without an emulator or a network.
 *
 * `Activity` appears in [showInterstitial] because the Google Mobile Ads
 * SDK genuinely requires one to present a full-screen ad — that is a
 * real constraint of the platform, not leakage, and it stops at the
 * interface rather than travelling further as an SDK type.
 */
interface AdsController {

    /**
     * Initialises the underlying SDK. Safe to call more than once;
     * subsequent calls are no-ops.
     *
     * Deliberately fire-and-forget from the caller's perspective:
     * initialisation performs disk and network I/O, and the composition
     * root must not block on it.
     */
    fun initialize()

    /** Whether ads may be shown at all in this build and consent state. */
    fun areAdsEnabled(): Boolean

    /**
     * Loads an interstitial ahead of a placement so it is ready when the
     * moment arrives. Doing this at request time would either delay the
     * user or, more likely, produce nothing.
     *
     * A no-op when the gate would refuse the placement anyway — there is
     * no point spending a user's data on an ad that will not be shown.
     */
    fun preloadInterstitial(placement: AdPlacement)

    /**
     * Shows a preloaded interstitial if one is ready and the gate allows
     * it. Returns whether an ad was actually displayed, so callers can
     * distinguish "shown" from "skipped" without inspecting SDK state.
     *
     * Never blocks and never queues: if no ad is ready, the moment
     * passes. An ad that arrives late is an ad that interrupts something
     * else.
     */
    fun showInterstitial(activity: Activity, placement: AdPlacement): Boolean

    /** The ad unit a banner placement should render, or null when
     *  banners are not permitted right now. */
    fun bannerAdUnitIdOrNull(placement: AdPlacement): String?
}

/**
 * The binding used whenever ads are switched off — debug builds by
 * default, and any build where consent has not been resolved.
 *
 * A real implementation rather than a test double: this is what runs in
 * production for every user the gate refuses, so it must behave
 * correctly and cheaply. It also means a feature module calling
 * `showInterstitial` never has to know whether ads exist in this build.
 */
class NoOpAdsController : AdsController {
    override fun initialize() = Unit
    override fun areAdsEnabled(): Boolean = false
    override fun preloadInterstitial(placement: AdPlacement) = Unit
    override fun showInterstitial(activity: Activity, placement: AdPlacement): Boolean = false
    override fun bannerAdUnitIdOrNull(placement: AdPlacement): String? = null
}
