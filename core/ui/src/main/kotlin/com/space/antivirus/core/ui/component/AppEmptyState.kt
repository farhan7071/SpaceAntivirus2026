package com.space.antivirus.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.space.antivirus.core.designsystem.theme.LocalSpacing

/**
 * Every empty state affirms the positive rather than reading as blank —
 * Sprint 002.5 §15 / Sprint 002.75 §10. This is the ONE component every
 * feature's empty state must use, so that principle can't be silently
 * dropped screen-by-screen.
 */
@Composable
fun AppEmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(text = message)
    }
}
