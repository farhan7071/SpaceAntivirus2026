package com.space.antivirus.core.designsystem.brand

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * The decorative field behind a hero card — Sprint 050.
 *
 * A gradient wash, a soft radial bloom, and two orbital arcs, all in a
 * single accent at very low opacity.
 *
 * `accent` defaults to the brand primary but is a parameter because
 * Home's hero is status-coloured: an "Attention needed" card washes
 * amber, a protected one green. Hard-coding primary here would have
 * quietly cost the Home hero its status meaning, which is the one thing
 * on that card carrying information rather than decoration. Applied to a hero card's
 * content `Modifier`; it paints behind, changing nothing about layout.
 *
 * **Why a component rather than three heroes each drawing their own.**
 * Home, the Cleaner and eventually Security Center all want the same
 * treatment. Three copies would drift within a sprint, and the whole
 * point of the effect is that a user recognises the same surface across
 * screens.
 *
 * **Why so faint.** Every alpha here is between 0.03 and 0.10. The brief
 * asks for decoration that reinforces identity without competing with
 * the headline, and the failure mode of decorative backdrops is that
 * they read fine in isolation and then fight the text once real content
 * lands on them. At these values the geometry is felt rather than
 * looked at — which is the difference between a premium surface and a
 * busy one.
 *
 * **Light and dark are not the same treatment.** Dark mode already has
 * depth from Material's elevation tint, so the backdrop is a touch
 * stronger there and does the work of texture. Light mode gets a wider,
 * softer bloom, because on a near-white surface a tight glow reads as a
 * smudge rather than as light.
 *
 * Nothing here touches the semantics tree — `drawBehind` is pure
 * painting, so there is no node for a screen reader to encounter in the
 * first place.
 */
/**
 * Applied as a `Modifier` rather than exposed as a layout, deliberately.
 *
 * A `Box`-and-backdrop component would have forced every hero to gain a
 * wrapper and re-indent its whole body — churn in three screens for a
 * purely visual effect, and a diff that buries the change everyone
 * actually needs to review. `drawBehind` paints under the existing
 * content with no layout change at all.
 */
@Composable
fun Modifier.heroBackdrop(accent: Color = MaterialTheme.colorScheme.primary): Modifier {
    val isDark = isSystemInDarkTheme()
    val surface = MaterialTheme.colorScheme.surface
    return this.drawBehind {
        drawGradientWash(accent = accent, surface = surface, isDark = isDark)
        drawBloom(accent = accent, isDark = isDark)
        drawOrbitalArcs(accent = accent, isDark = isDark)
    }
}

/**
 * A diagonal wash rather than a flat fill.
 *
 * The flat `primary.copy(alpha = …)` this replaces gave the card one
 * uniform tone edge to edge, which is what made an otherwise
 * well-proportioned hero read as a coloured rectangle. A gradient gives
 * the surface a direction, and direction is most of what separates a
 * panel from a swatch.
 */
private fun DrawScope.drawGradientWash(accent: Color, surface: Color, isDark: Boolean) {
    val topAlpha = if (isDark) 0.20f else 0.14f
    val bottomAlpha = if (isDark) 0.06f else 0.03f
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(accent.copy(alpha = topAlpha), accent.copy(alpha = bottomAlpha)),
            start = Offset.Zero,
            end = Offset(x = size.width, y = size.height),
        ),
    )
    // A whisper of surface along the bottom edge, so the wash resolves
    // into the card rather than stopping at it.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, surface.copy(alpha = if (isDark) 0.10f else 0.35f)),
            startY = size.height * 0.55f,
            endY = size.height,
        ),
    )
}

/**
 * The radial bloom, placed off-centre toward the top right — behind
 * where the brand mark sits in every hero that uses this. It reads as
 * light coming from the mark rather than as a circle placed near it.
 */
private fun DrawScope.drawBloom(accent: Color, isDark: Boolean) {
    val center = Offset(x = size.width * 0.82f, y = size.height * 0.26f)
    val radius = size.minDimension * if (isDark) 0.85f else 1.05f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = if (isDark) 0.22f else 0.16f), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * Two orbital arcs, tilted to match the brand mark's own ring.
 *
 * Drawn as very wide ellipses that mostly fall outside the card and get
 * clipped by it, so what remains on screen are two long shallow curves
 * rather than two visible ovals. That is the difference between orbital
 * geometry and a pair of circles.
 */
private fun DrawScope.drawOrbitalArcs(accent: Color, isDark: Boolean) {
    val strokeWidth = size.minDimension * 0.012f
    val alpha = if (isDark) 0.10f else 0.07f

    rotate(degrees = ORBIT_TILT_DEGREES, pivot = Offset(size.width * 0.78f, size.height * 0.30f)) {
        listOf(1.0f, 1.45f).forEach { scale ->
            val ovalWidth = size.width * 1.15f * scale
            val ovalHeight = size.height * 0.80f * scale
            drawOval(
                color = accent.copy(alpha = alpha),
                topLeft = Offset(
                    x = size.width * 0.78f - ovalWidth / 2f,
                    y = size.height * 0.30f - ovalHeight / 2f,
                ),
                size = Size(width = ovalWidth, height = ovalHeight),
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

/** Matches SpaceBrandMark's own ring, so the backdrop's geometry and the
 *  mark's read as one system rather than two accidents. */
private const val ORBIT_TILT_DEGREES = -20f
