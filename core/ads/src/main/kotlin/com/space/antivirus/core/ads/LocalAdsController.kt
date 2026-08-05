package com.space.antivirus.core.ads

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Supplies the app's [AdsController] to composables — Sprint 044.
 *
 * A composition local rather than a constructor parameter threaded down
 * from `MainActivity`, for one reason: an ad is not a screen's concern.
 * A History screen should not gain a parameter, and its ViewModel should
 * not gain a dependency, because somewhere below it there is a banner.
 * Adding or removing a placement stays a change in the ads module plus
 * the one composable that renders it.
 *
 * Defaults to [NoOpAdsController], so a preview, a test, or any
 * composable rendered outside the app's provider shows no ads and needs
 * no setup — the same fail-closed default the rest of this module uses.
 */
val LocalAdsController = staticCompositionLocalOf<AdsController> { NoOpAdsController() }
