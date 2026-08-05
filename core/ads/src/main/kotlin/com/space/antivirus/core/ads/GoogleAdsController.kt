package com.space.antivirus.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.space.antivirus.domain.support.AppInfoProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one class in this project that touches the Google Mobile Ads SDK —
 * Sprint 044.
 *
 * Everything policy-shaped lives in [AdsGate], which is plain Kotlin and
 * tested. This class is deliberately mechanical: load, hold, show,
 * report. That split is what makes the interesting behaviour testable
 * without an emulator.
 *
 * **Consent is read from [AdsGate]'s [ConsentState], which defaults to
 * UNKNOWN and therefore blocks ads.** Until the UMP SDK is integrated,
 * this controller will not serve anything. That is intentional — see
 * `ConsentState`'s own documentation for why failing closed is the only
 * safe default here.
 */
@Singleton
class GoogleAdsController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adsGate: AdsGate,
    private val appInfoProvider: AppInfoProvider,
    private val consentProvider: ConsentProvider,
) : AdsController {

    private val initialized = AtomicBoolean(false)
    private var loadedInterstitial: InterstitialAd? = null
    private var interstitialLoadInFlight = false

    private val adsEnabledForBuild: Boolean
        get() = AdsConfig.adsEnabled(isDebugBuild = appInfoProvider.getAppInfo().isDebugBuild)

    override fun initialize() {
        if (!adsEnabledForBuild) return
        // compareAndSet, not a plain boolean: MobileAds.initialize is
        // reachable from the composition root and from a preload call
        // that may race it on a different thread.
        if (!initialized.compareAndSet(false, true)) return

        // The SDK performs disk and network I/O on this call and hands
        // back its result on the main thread. Nothing here waits on it;
        // ad requests made before it completes are queued by the SDK.
        MobileAds.initialize(context) { }
        adsGate.onAppStarted()
    }

    override fun areAdsEnabled(): Boolean =
        adsGate.canShowBanner(consentProvider.currentConsent(), adsEnabledForBuild)

    override fun preloadInterstitial(placement: AdPlacement) {
        if (placement != AdPlacement.SCAN_COMPLETE_INTERSTITIAL) return
        if (!adsGate.canShowInterstitial(consentProvider.currentConsent(), adsEnabledForBuild)) return
        if (loadedInterstitial != null || interstitialLoadInFlight) return

        interstitialLoadInFlight = true
        InterstitialAd.load(
            context,
            AdsConfig.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoadInFlight = false
                    loadedInterstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Swallowed deliberately: a failed ad load is not a
                    // user-facing problem and must never surface as one
                    // in a security app.
                    interstitialLoadInFlight = false
                    loadedInterstitial = null
                }
            },
        )
    }

    override fun showInterstitial(activity: Activity, placement: AdPlacement): Boolean {
        if (placement != AdPlacement.SCAN_COMPLETE_INTERSTITIAL) return false
        if (!adsGate.canShowInterstitial(consentProvider.currentConsent(), adsEnabledForBuild)) return false

        val ad = loadedInterstitial ?: return false
        // Cleared before showing, not after: an ad is single-use, and
        // holding a reference past display risks showing it twice.
        loadedInterstitial = null

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // Recorded on dismissal rather than on show, so the
                // quiet period starts when the user got their screen
                // back.
                adsGate.recordInterstitialShown()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                // Never displayed, so the user's quiet period is not
                // consumed.
                loadedInterstitial = null
            }
        }
        ad.show(activity)
        return true
    }

    override fun bannerAdUnitIdOrNull(placement: AdPlacement): String? {
        if (placement != AdPlacement.HISTORY_BANNER) return null
        if (!adsGate.canShowBanner(consentProvider.currentConsent(), adsEnabledForBuild)) return null
        return AdsConfig.BANNER_AD_UNIT_ID
    }
}

/**
 * Supplies the current consent position — Sprint 044.
 *
 * The seam the UMP SDK will eventually implement. The default binding
 * returns UNKNOWN, which blocks every ad.
 */
interface ConsentProvider {
    fun currentConsent(): ConsentState
}

/**
 * The production binding until UMP is integrated.
 *
 * Returns UNKNOWN, so no ad is served by any build. This is not a stub
 * left half-finished — it is the correct, deliberate behaviour for an
 * app that has not yet asked for consent, and swapping it for a real UMP
 * implementation is a one-line change in the Hilt module.
 */
@Singleton
class UnresolvedConsentProvider @Inject constructor() : ConsentProvider {
    override fun currentConsent(): ConsentState = ConsentState.UNKNOWN
}
