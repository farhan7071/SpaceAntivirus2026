package com.space.antivirus.core.ui.component

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Sprint 030 — loads a real app icon via PackageManager at display time,
 * keyed only by packageName (already persisted on Threat since Sprint
 * 029 as targetIdentifier). Deliberately NOT persisted or added as a new
 * Threat field: an icon is large, mutable (apps update their icons), and
 * entirely re-derivable from a field that already exists — the same
 * "derive at display time, don't persist what's already derivable"
 * reasoning ADR 0043 already applied to recommendationFor/shortSummaryFor.
 *
 * Renders via AndroidView + a plain ImageView (Compose's own, official
 * interop mechanism for embedding a traditional Android View, chosen
 * deliberately over manual Drawable-to-Bitmap conversion — that path
 * needs either a new core-ktx dependency this module doesn't currently
 * have, or hand-written Canvas code, both real, avoidable risk for a
 * problem AndroidView already solves safely). ImageView.setImageDrawable
 * already handles every Drawable subtype an app's PackageManager icon
 * can be (bitmap, adaptive, vector) — Compose does not need to know
 * which.
 *
 * Falls back to the app's first letter on a colored circle if the app is
 * no longer installed (a real, expected case — History shows scans from
 * apps that may have been uninstalled since) or the icon can't be
 * loaded for any other reason. Never shows a broken image or blank
 * space — the fallback is deliberately still informative (the letter),
 * not a generic placeholder icon.
 */
@Composable
fun AppIcon(packageName: String, appLabel: String, modifier: Modifier = Modifier, sizeDp: Int = 40) {
    val context = LocalContext.current
    val drawable by produceState<Drawable?>(initialValue = null, packageName) {
        value = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    val loadedDrawable = drawable
    if (loadedDrawable != null) {
        AndroidView(
            factory = { viewContext -> ImageView(viewContext) },
            update = { imageView -> imageView.setImageDrawable(loadedDrawable) },
            modifier = modifier
                .size(sizeDp.dp)
                .clip(CircleShape),
        )
    } else {
        AppIconFallback(appLabel = appLabel, modifier = modifier, sizeDp = sizeDp)
    }
}

@Composable
private fun AppIconFallback(appLabel: String, modifier: Modifier = Modifier, sizeDp: Int = 40) {
    val initial = appLabel.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
