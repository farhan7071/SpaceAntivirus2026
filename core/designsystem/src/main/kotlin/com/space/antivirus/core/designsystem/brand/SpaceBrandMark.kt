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
 * A shield containing a planet and its orbital ring — the finalised
 * launcher icon, redrawn as a path. Drawn rather than shipped as a raster asset so it scales to
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
 * The ring is elliptical and tilted rather than a plain circle. A
 * concentric ring reads as a loading spinner, which is the last thing a
 * brand mark should suggest in an app that also shows real progress
 * indicators.
 */
@Composable
fun SpaceBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    contentDescription: String? = null,
    emphasis: BrandMarkEmphasis = BrandMarkEmphasis.FULL,
) {
    val isDark = isSystemInDarkTheme()
    val shieldColor = MaterialTheme.colorScheme.primary.copy(alpha = emphasis.alpha)
    // The orbit sits behind the shield and must read as secondary to it.
    // Alpha rather than a second colour token: it stays correct against
    // any surface the mark is placed on, which a fixed tint would not.
    val glowColor = shieldColor.copy(alpha = shieldColor.alpha * if (isDark) 0.18f else 0.10f)
    // Planet and ring, in the shield's contrasting colour — the same
    // two-tone treatment as the identity sheet's monochrome variant.
    // Follows the shield's own emphasis rather than staying opaque: at
    // LOW the detail would otherwise be the brightest thing in the mark,
    // inverting the hierarchy the emphasis level is asking for.
    val detailColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = emphasis.alpha)

    val semanticsModifier = contentDescription
        ?.let { description -> Modifier.semantics { this.contentDescription = description } }
        ?: Modifier

    Canvas(modifier = modifier.size(size).then(semanticsModifier)) {
        drawGlow(glowColor)
        drawShieldWithPlanet(shieldColor = shieldColor, detailColor = detailColor)
    }
}

/** A soft halo so the mark has presence on a flat surface without
 *  needing a card behind it. */
private fun DrawScope.drawGlow(color: Color) {
    drawCircle(color = color, radius = this.size.minDimension * 0.48f)
}

/**
 * The canonical mark: a heater shield containing a planet with an
 * orbital ring — Sprint 048.
 *
 * **What changed and why.** Sprint 045 drew a shield with a checkmark,
 * because at the time there was no finalised identity and a checkmark is
 * the obvious security glyph. The launcher icon is now the shield with a
 * planet and orbit, and two different marks for one app is worse than
 * either mark on its own. The geometry below is the icon's, redrawn — no
 * bitmap, still a Canvas path, still theme-tinted.
 *
 * Losing the checkmark also settles the tension Sprint 046.1 had to work
 * around with `BrandMarkEmphasis.LOW`. A shield with a check in it
 * asserts that the device is fine, which is why placing it beside a live
 * "Attention needed" headline needed damping. A planet asserts nothing —
 * it is a logo, not a verdict. LOW is kept because the mark should still
 * sit quietly next to live status, but it is now a hierarchy choice
 * rather than a correction for a contradictory claim.
 *
 * The shield is wider and taller than the checkmark version, because it
 * now has to contain something: at the old 0.30-0.70 width a planet plus
 * ring would have been a smudge at 36dp.
 */
private fun DrawScope.drawShieldWithPlanet(shieldColor: Color, detailColor: Color) {
    val width = this.size.width
    val height = this.size.height

    val left = width * 0.20f
    val right = width * 0.80f
    val top = height * 0.17f
    val bottom = height * 0.86f
    val shoulder = height * 0.55f

    val shield = Path().apply {
        moveTo(left, top)
        lineTo(right, top)
        lineTo(right, shoulder)
        // The flanks curve inward to the point rather than meeting it in
        // a straight V, which would read as an arrow.
        quadraticTo(right, bottom - height * 0.08f, width * 0.5f, bottom)
        quadraticTo(left, bottom - height * 0.08f, left, shoulder)
        close()
    }
    drawPath(path = shield, color = shieldColor)

    val planetCenter = Offset(x = width * 0.5f, y = height * 0.46f)
    val planetRadius = width * PLANET_RADIUS_FRACTION
    drawCircle(color = detailColor, radius = planetRadius, center = planetCenter)

    // The ring is drawn fully over the planet rather than threaded behind
    // it. Correct occlusion would need an offscreen layer and a clip per
    // half, and at the sizes this mark is actually used — 36dp in the
    // Home hero, 112dp on onboarding — the overlap is a pixel or two.
    // The identity sheet's own notification and monochrome variants draw
    // it flat for the same reason.
    val ringStroke = width * ORBIT_STROKE_FRACTION
    rotate(degrees = ORBIT_TILT_DEGREES, pivot = planetCenter) {
        val ringWidth = planetRadius * 2.9f
        val ringHeight = planetRadius * 1.05f
        drawOval(
            color = detailColor,
            topLeft = Offset(
                x = planetCenter.x - ringWidth / 2f,
                y = planetCenter.y - ringHeight / 2f,
            ),
            size = Size(width = ringWidth, height = ringHeight),
            style = Stroke(width = ringStroke),
        )
        // The bead on the ring. Small, and the one asymmetry that stops
        // the mark reading as a generic planet glyph.
        drawCircle(
            color = detailColor,
            radius = ringStroke * 1.6f,
            center = Offset(x = planetCenter.x + ringWidth / 2f, y = planetCenter.y),
        )
    }
}

private const val ORBIT_TILT_DEGREES = -20f
private const val ORBIT_STROKE_FRACTION = 0.038f
private const val PLANET_RADIUS_FRACTION = 0.115f

/**
 * How loudly the mark should speak — Sprint 046.1.
 *
 * Added when the mark stopped being an onboarding illustration and became
 * the application's identity, appearing alongside live status.
 *
 * The distinction matters because of what the mark contains: a shield
 * with a check in it. At [FULL] that reads as an affirmative statement,
 * which is correct on a splash or an onboarding page where the mark IS
 * the content. Placed beside a live "Attention needed" headline it would
 * be making a second, contradictory claim about the same device. At [LOW]
 * it is a monochrome watermark at roughly a third of the surrounding
 * contrast — recognisably the app's mark, the way Gmail's envelope sits
 * in a toolbar without asserting anything about your inbox.
 *
 * A hierarchy control, not a decoration knob. A mark that competes with
 * the status it sits beside is worse than no mark.
 */
enum class BrandMarkEmphasis(val alpha: Float) {

    /** Splash, onboarding, About — the mark is the content. */
    FULL(alpha = 1f),

    /** Alongside live status. Present, never competing. */
    LOW(alpha = 0.35f),
}
