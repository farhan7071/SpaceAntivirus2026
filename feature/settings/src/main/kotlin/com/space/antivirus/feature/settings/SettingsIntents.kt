package com.space.antivirus.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Every system hand-off the Settings section performs — Sprint 043A.
 *
 * Kept out of the Composables and out of the ViewModels both: a
 * ViewModel never holds a Context in this project, and a Composable
 * shouldn't build Intents. Same placement as Security Center's App Info
 * and uninstall helpers.
 *
 * Every one of these can fail on a real device — no Play Store on an
 * AOSP or Huawei build, no email client, an OEM that omits the battery
 * screen. All of them fail quietly and return false rather than
 * crashing: none is load-bearing, and a settings row that does nothing
 * is a far better outcome than a crash from tapping "Rate app".
 */
internal object SettingsIntents {

    fun openUrl(context: Context, url: String): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    /**
     * Prefers the installed Play Store app, falling back to the web
     * listing. Both can be absent, which is why the result is returned.
     */
    fun openPlayStoreListing(context: Context, packageName: String): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(SupportLinks.playStoreMarketUri(packageName)))) ||
            openUrl(context, SupportLinks.playStoreWebUrl(packageName))

    fun shareApp(context: Context, packageName: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, SupportLinks.shareMessage(packageName))
        }
        return start(context, Intent.createChooser(send, "Share Space Antivirus"))
    }

    /**
     * ACTION_SENDTO with a `mailto:` URI rather than ACTION_SEND: it
     * resolves only to actual email clients, so the user isn't offered a
     * chooser full of messaging apps for something addressed to a
     * support inbox.
     */
    fun sendFeedback(context: Context, appInfoLine: String): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${SupportLinks.FEEDBACK_EMAIL}")
            putExtra(Intent.EXTRA_SUBJECT, SupportLinks.FEEDBACK_SUBJECT)
            // Version details included so a support reply doesn't have to
            // start by asking which build the user is on.
            putExtra(Intent.EXTRA_TEXT, "\n\n---\n$appInfoLine")
        }
        return start(context, intent)
    }

    /**
     * Opens the system's battery-optimisation list. Deliberately never
     * ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, which puts up a
     * direct "allow this app to always run" prompt and is Play-restricted
     * to a narrow set of app categories (ADR 0055).
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean =
        start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

    /**
     * The system's own notification settings for this app — where the
     * per-channel controls actually live. Android owns channel
     * enablement once a channel exists; an in-app mirror of those
     * switches would be a second source of truth that the OS can change
     * behind our back.
     */
    fun openAppNotificationSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
