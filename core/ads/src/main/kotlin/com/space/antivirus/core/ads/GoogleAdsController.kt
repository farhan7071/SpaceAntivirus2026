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
 * **Sprint 049: consent is real.** This depends on `UmpConsentManager`
 * concretely rather than on the `ConsentProvider` interface, because it
 * needs `gatherConsent` — running the flow — and not merely the current
 * answer. Everything that only needs the answer, `AdsGate` included,
 * still depends on the interface.
 */
@Singleton
class GoogleAdsController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adsGate: AdsGate,
    private val appInfoProvider: AppInfoProvider,
    private val consentProvider: UmpConsentManager,
) : AdsController {

    private val initialized = AtomicBoolean(false)
    private var loadedInterstitial: InterstitialAd? = null
    private var interstitialLoadInFlight = false

    private val adsEnabledForBuild: Boolean
        get() = AdsConfig.adsEnabled(isDebugBuild = appInfoProvider.getAppInfo().isDebugBuild)

    override fun gatherConsentAndInitialize(activity: Activity) {
        // A debug build asks for nothing and initialises nothing. There
        // is no consent question to put to a developer whose build will
        // never request an ad, and a form appearing on every debug
        // launch would be noise.
        if (!adsEnabledForBuild) return

        consentProvider.gatherConsent(activity) { canRequestAds ->
            // Sprint 049: the SDK is initialised only once consent has
            // actually come back permitting ads. Initialising first and
            // gating requests afterwards — Sprint 044's shape — is not
            // sufficient under the EU User Consent Policy, which governs
            // initialisation, not just the request.
            if (!canRequestAds) return@gatherConsent

            // compareAndSet, not a plain boolean: this callback and a
            // preload racing it can arrive on different threads.
            if (!initialized.compareAndSet(false, true)) return@gatherConsent

            // Performs disk and network I/O, and hands its result back
            // on the main thread. Nothing waits on it; requests made
            // before it completes are queued by the SDK.
            MobileAds.initialize(context) { }
            adsGate.onAppStarted()
        }
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
 * Supplies the current consent position.
 *
 * Sprint 044 introduced this as a seam with a deliberately blocking
 * placeholder behind it. Sprint 049 put `UmpConsentManager` behind it
 * instead and deleted the placeholder; the interface itself did not have
 * to change, which was the point of having it.
 */
interface ConsentProvider {
    fun currentConsent(): ConsentState
}
