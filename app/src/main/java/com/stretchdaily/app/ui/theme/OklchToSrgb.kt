package com.stretchdaily.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Converts an oklch color to sRGB (Compose [Color]).
 *
 * Pipeline (per CSS Color 4):
 *   1. oklch → oklab  (C, h_deg → a, b using polar → cartesian)
 *   2. oklab → linear sRGB  (3×3 matrix + cube step)
 *   3. linear sRGB → sRGB  (gamma correction)
 *
 * @param l Lightness 0..1 (oklab L).
 * @param c Chroma 0..~0.37 (oklab C).
 * @param hDeg Hue in degrees 0..360.
 */
fun oklchToSrgb(l: Double, c: Double, hDeg: Double): Color {
    val hRad = hDeg * PI / 180.0
    val a = c * cos(hRad)
    val b = c * sin(hRad)
    val (r, g, blue) = oklabToLinearSrgb(l, a, b)
    return Color(
        red = linearToSrgbChannel(r).toFloat(),
        green = linearToSrgbChannel(g).toFloat(),
        blue = linearToSrgbChannel(blue).toFloat(),
        alpha = 1.0f,
    )
}

/** oklab → linear sRGB per CSS Color 4 reference. */
private fun oklabToLinearSrgb(l: Double, a: Double, b: Double): Triple<Double, Double, Double> {
    // oklab → LMS (cube)
    val lL = l + 0.3963377774 * a + 0.2158037573 * b
    val lM = l - 0.1055613458 * a - 0.0638541728 * b
    val lS = l - 0.0894841775 * a - 1.2914855480 * b

    val lCubed = lL * lL * lL
    val mCubed = lM * lM * lM
    val sCubed = lS * lS * lS

    // LMS → linear sRGB
    val r = +4.0767416621 * lCubed - 3.3077115913 * mCubed + 0.2309699292 * sCubed
    val g = -1.2684380046 * lCubed + 2.6097574011 * mCubed - 0.3413193965 * sCubed
    val blue = -0.0041960863 * lCubed - 0.7034186147 * mCubed + 1.7076147010 * sCubed

    return Triple(r, g, blue)
}

/** Gamma-encodes linear sRGB to display-referred sRGB and clamps to [0, 1]. */
private fun linearToSrgbChannel(v: Double): Double {
    val clamped = v.coerceIn(0.0, 1.0)
    val encoded = if (clamped <= 0.0031308) {
        clamped * 12.92
    } else {
        1.055 * clamped.pow(1.0 / 2.4) - 0.055
    }
    // Unused-variable-silencer: we only use `encoded` — kept explicit for readability.
    return encoded
}

// Kept private by default — if future consumers need raw oklab, expose it then.
private inline fun <A, B, C> trip(a: A, b: B, c: C) = Triple(a, b, c)

// Unused cbrt import kept because alternate conversion formulations use it;
// remove if you confirm the Triple-based path above is the final form.
@Suppress("unused")
private val unusedCbrtHook: (Double) -> Double = ::cbrt
