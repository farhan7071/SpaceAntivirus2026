package com.space.antivirus.core.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * The "recovery" action referenced throughout Sprint 002.75 (e.g. "Turn on
 * storage access" -> deep-links to system settings). One implementation,
 * used by every feature's permission-missing card.
 */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
