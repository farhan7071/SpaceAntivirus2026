package com.space.antivirus.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * M3 shape scale. Cards default to "medium" (per Sprint 002.5 §8); the
 * scan-progress hero element animates across this scale via shape
 * morphing rather than using a fixed shape — implemented in
 * feature:security, not here (this module only defines the static scale).
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
