package com.space.antivirus.core.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * The ONE confirmation-dialog component, per Sprint 002.5 §9 — replaces
 * the current app's ad hoc dialog set (dialog_junk_limit, dialog_stop_scan,
 * etc. per Sprint 001). Reserved for genuinely blocking confirmations only
 * (Sprint 002.5 §8) — never used for informational content.
 */
@Composable
fun AppConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { AppTextButton(text = confirmLabel, onClick = onConfirm) },
        dismissButton = { AppTextButton(text = dismissLabel, onClick = onDismiss) },
    )
}
