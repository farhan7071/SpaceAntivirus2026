package com.space.antivirus.core.ui.component

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Determinate-first per Sprint 002.5 §8: progress is shown as a real
 * percentage wherever the underlying process reports one (scan, clean).
 * Indeterminate is reserved for genuinely unknown-duration waits
 * (purchase processing) — see Sprint 002.5 §8.
 */
@Composable
fun AppCircularProgress(progress: Float?, modifier: Modifier = Modifier) {
    if (progress != null) {
        CircularProgressIndicator(progress = { progress }, modifier = modifier)
    } else {
        CircularProgressIndicator(modifier = modifier)
    }
}

@Composable
fun AppLinearProgress(progress: Float?, modifier: Modifier = Modifier) {
    if (progress != null) {
        LinearProgressIndicator(progress = { progress }, modifier = modifier)
    } else {
        LinearProgressIndicator(modifier = modifier)
    }
}
