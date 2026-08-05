package com.space.antivirus.viruscleaner.mobilesecurity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.space.antivirus.core.ads.AdPlacement
import com.space.antivirus.core.ads.AdsController
import com.space.antivirus.core.ads.LocalAdsController
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import com.space.antivirus.viruscleaner.mobilesecurity.navigation.SpaceAntivirusNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-Activity host per Sprint 002 section 7's Compose migration decision.
 * Replaces the old app's MainActivity + PermissionHelpActivity two-Activity
 * pattern (Sprint 001) — permission rationale is now an in-nav-graph
 * screen/dialog, not a separate transparent Activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Sprint 044. The Activity is where ads are held because the Mobile
     * Ads SDK genuinely needs one to present a full-screen ad, and this
     * app is single-Activity by design (see this class's KDoc above).
     */
    @Inject
    lateinit var adsController: AdsController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Loaded ahead of the placement rather than at request time: an
        // interstitial requested at the moment it is needed either
        // delays the user or, more likely, returns nothing. A no-op
        // whenever the gate would refuse it anyway, so this costs a
        // blocked user nothing.
        adsController.preloadInterstitial(AdPlacement.SCAN_COMPLETE_INTERSTITIAL)

        setContent {
            SpaceAntivirusRoot(
                adsController = adsController,
                onScanResultAcknowledged = {
                    adsController.showInterstitial(this, AdPlacement.SCAN_COMPLETE_INTERSTITIAL)
                    // Reload for the next eligible moment. The gate, not
                    // this call, decides whether it will ever be shown.
                    adsController.preloadInterstitial(AdPlacement.SCAN_COMPLETE_INTERSTITIAL)
                },
            )
        }
    }

    // DIAGNOSTIC (Sprint 32.4) — temporary, remove before release.
    // Overriding this Activity's own lifecycle methods directly, rather
    // than registering an Application.ActivityLifecycleCallbacks on
    // SpaceAntivirusApp, deliberately — this app is single-Activity by
    // design (this class's own KDoc above), so both approaches observe
    // identical information here, and this avoids touching
    // SpaceAntivirusApp.onCreate() at all, whose WorkManager/Hilt
    // initialization ordering is already deliberately fragile and
    // carefully reasoned (ADR 0040) — no diagnostic addition belongs
    // anywhere near it if it doesn't have to be. Purpose: during an
    // uninstall attempt, this answers whether the system Package
    // Installer actually took foreground (onPause, likely onStop, then
    // onResume only once the user returns) or immediately handed control
    // back (onPause immediately followed by onResume, with little or no
    // time and no onStop in between).
    override fun onPause() {
        Log.d("OverflowMenuDiag", "MainActivity.onPause()")
        super.onPause()
    }

    override fun onStop() {
        Log.d("OverflowMenuDiag", "MainActivity.onStop()")
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        Log.d("OverflowMenuDiag", "MainActivity.onResume()")
    }
}

@Composable
private fun SpaceAntivirusRoot(
    adsController: AdsController,
    onScanResultAcknowledged: () -> Unit,
) {
    SpaceAntivirusTheme {
        // Provided once at the root so no screen gains an ads parameter
        // and no ViewModel gains an ads dependency just because a banner
        // sits somewhere beneath it.
        CompositionLocalProvider(LocalAdsController provides adsController) {
            Surface(modifier = Modifier.fillMaxSize()) {
                val navController = rememberNavController()
                SpaceAntivirusNavHost(
                    navController = navController,
                    onScanResultAcknowledged = onScanResultAcknowledged,
                )
            }
        }
    }
}
