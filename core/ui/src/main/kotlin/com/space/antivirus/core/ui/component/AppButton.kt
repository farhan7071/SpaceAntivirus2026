package com.space.antivirus.core.ui.component

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The three button variants defined in Sprint 002.5 §8 — filled/outlined/
 * text. Feature modules must use these, not raw Material3 Button, so a
 * future visual change (e.g. shape morphing on press) happens in one place.
 * See Sprint 002.75 §5: verb-first, 1-3 word labels.
 */
@Composable
fun AppFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text)
    }
}

@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text)
    }
}

@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text)
    }
}
