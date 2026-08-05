package com.space.antivirus.core.ads.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.space.antivirus.core.ads.AdPlacement
import com.space.antivirus.core.ads.AdsController

/**
 * A banner, or nothing at all — Sprint 044.
 *
 * Lives in `core:ads` rather than `core:ui` so `AdView` and the rest of
 * the Google SDK stay inside the one module allowed to see them. A
 * feature module composes `AdBanner(placement = ...)` and never imports
 * a Google class.
 *
 * **When banners are not permitted this emits nothing — not an empty
 * reserved box.** A blank rectangle where an ad failed to load looks
 * like a rendering bug, and in a security app anything that looks broken
 * costs more than the ad was worth. The layout simply closes up.
 *
 * The `AdView` is created once by `AndroidView`'s factory and destroyed
 * in `onRelease`. Recreating it per recomposition would request a new ad
 * each time — billable, pointless, and the kind of pattern that trips
 * invalid-traffic detection.
 */
@Composable
fun AdBanner(
    placement: AdPlacement,
    adsController: AdsController,
    modifier: Modifier = Modifier,
) {
    val adUnitId = adsController.bannerAdUnitIdOrNull(placement) ?: return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        // An AdView that outlives its screen keeps refreshing and leaks
        // its Context.
        onRelease = { adView -> adView.destroy() },
    )
}
