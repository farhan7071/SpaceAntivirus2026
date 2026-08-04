package com.space.antivirus.core.protection

import android.content.Context
import android.os.PowerManager
import com.space.antivirus.domain.protection.BatteryOptimizationStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the standard Android battery-optimisation allowlist — Sprint 042.
 *
 * Read-only, and that is the whole design. There is no companion
 * "request exemption" method here, because this app does not ask: the
 * direct-request Intent is Play-restricted to a narrow set of app
 * categories, and a security app that nags its way onto the unrestricted
 * list is doing the thing that gives this category its reputation. The
 * UI shows a one-time informational card and links to the system's own
 * settings screen, where the user decides.
 */
@Singleton
class AndroidBatteryOptimizationStatus @Inject constructor(
    @ApplicationContext private val context: Context,
) : BatteryOptimizationStatus {

    override fun isIgnoringBatteryOptimizations(): Boolean = try {
        val powerManager = context.getSystemService(PowerManager::class.java)
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } catch (e: Exception) {
        // A device that won't answer is not a device we should claim is
        // exempt — false means "we can't confirm you're unrestricted",
        // which is the honest reading either way.
        false
    }
}
