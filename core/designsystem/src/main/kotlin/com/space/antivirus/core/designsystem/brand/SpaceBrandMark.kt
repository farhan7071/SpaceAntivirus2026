package com.space.antivirus.core.designsystem.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Space Antivirus mark — Sprint 045.
 *
 * A shield inside an orbit: the two halves of the name, in the brand's
 * own teal. Drawn rather than shipped as a raster asset so it scales to
 * any density without a set of PNGs, re-tints itself for light and dark
 * from the theme rather than needing two files, and adds nothing to the
 * APK.
 *
 * **Geometry is proportional, never absolute.** Every coordinate below
 * is a fraction of the canvas, so the same code draws a 40dp mark in an
 * empty state and a 120dp mark on onboarding with identical weight —
 * including the stroke widths, which scale with the canvas instead of
 * going spindly at large sizes and muddy at small ones.
 *
 * The orbit is deliberately elliptical and tilted rather than a plain
 * ring. A concentric circle around a shield reads as a loading spinner,
 * which is the last thing a brand mark should suggest in an app that
 * also shows real progress indicators.
 */
@Composable
fun SpaceBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    contentDescription: String? = null,
) {
    val isDark = isSystemInDarkTheme()
    val shieldColor = MaterialTheme.colorScheme.primary
    // The orbit sits behind the shield and must read as secondary to it.
    // Alpha rather than a second colour token: it stays correct against
    // any surface the mark is placed on, which a fixed tint would not.
    val orbitColor = shieldColor.copy(alpha = if (isDark) 0.55f else 0.40f)
    val glowColor = shieldColor.copy(alpha = if (isDark) 0.18f else 0.10f)
    val checkColor = MaterialTheme.colorScheme.onPrimary

    val semanticsModifier = contentDescription
        ?.let { description -> Modifier.semantics { this.contentDescription = description } }
        ?: Modifier

    Canvas(modifier = modifier.size(size).then(semanticsModifier)) {
        drawGlow(glowColor)
        drawOrbit(orbitColor)
        drawShield(color = shieldColor, checkColor = checkColor)
    }
}

/** A soft halo so the mark has presence on a flat surface without
 *  needing a card behind it. */
private fun DrawScope.drawGlow(color: Color) {
    drawCircle(color = color, radius = this.size.minDimension * 0.48f)
}

private fun DrawScope.drawOrbit(color: Color) {
    val width = this.size.width
    val height = this.size.height
    val strokeWidth = width * ORBIT_STROKE_FRACTION

    rotate(degrees = ORBIT_TILT_DEGREES) {
        drawOval(
            color = color,
            topLeft = Offset(x = width * 0.02f, y = height * 0.30f),
            size = Size(width = width * 0.96f, height = height * 0.40f),
            style = Stroke(width = strokeWidth),
        )
    }
}

/**
 * A classic heater shield: flat shoulders, straight flanks, and a point.
 * Built as an explicit path rather than a rounded rectangle so the
 * silhouette is recognisable at 24dp, where a rounded rectangle would
 * just read as a rounded rectangle.
 */
private fun DrawScope.drawShield(color: Color, checkColor: Color) {
    val width = this.size.width
    val height = this.size.height

    val left = width * 0.30f
    val right = width * 0.70f
    val top = height * 0.26f
    val bottom = height * 0.78f
    val shoulder = height * 0.52f

    val shield = Path().apply {
        moveTo(left, top)
        lineTo(right, top)
        lineTo(right, shoulder)
        // The flanks curve inward to the point rather than meeting it in
        // a straight V, which would read as an arrow.
        quadraticTo(right, bottom - height * 0.06f, width * 0.5f, bottom)
        quadraticTo(left, bottom - height * 0.06f, left, shoulder)
        close()
    }
    drawPath(path = shield, color = color)

    // Drawn in onPrimary on top of the shield rather than punched out of
    // it with a clear blend mode: a clear blend needs its own offscreen
    // layer to behave predictably, and one drawn stroke is both cheaper
    // and correct against every surface this mark is placed on.
    val check = Path().apply {
        moveTo(width * 0.40f, height * 0.50f)
        lineTo(width * 0.47f, height * 0.58f)
        lineTo(width * 0.61f, height * 0.40f)
    }
    drawPath(
        path = check,
        color = checkColor,
        style = Stroke(width = width * CHECK_STROKE_FRACTION, cap = StrokeCap.Round),
    )
}

private const val ORBIT_TILT_DEGREES = -20f
private const val ORBIT_STROKE_FRACTION = 0.045f
private const val CHECK_STROKE_FRACTION = 0.06f
