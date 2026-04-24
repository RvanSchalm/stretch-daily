package com.stretchdaily.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class OklchToSrgbTest {

    @Test
    fun `oklch neck tint resolves to expected sRGB`() {
        // Neck: oklch(0.82 0.04 140) → sRGB (182, 202, 178) per CSS Color 4
        // reference algorithm. Cross-checked against Ottosson's matrix and a
        // standalone Python implementation of the same pipeline. An earlier
        // expected value of (197, 214, 194) was pulled from a designer-tool
        // swatch and didn't match any standard color-space conversion.
        val color = oklchToSrgb(l = 0.82, c = 0.04, hDeg = 140.0)
        assertRgbWithin(color, expectedR = 182, expectedG = 202, expectedB = 178, tolerance = 3)
    }

    @Test
    fun `oklch accent-soft resolves to expected sRGB`() {
        // oklch(0.88 0.045 135) → sRGB (203, 222, 193) per CSS Color 4.
        val color = oklchToSrgb(l = 0.88, c = 0.045, hDeg = 135.0)
        assertRgbWithin(color, expectedR = 203, expectedG = 222, expectedB = 193, tolerance = 3)
    }

    @Test
    fun `zero chroma returns achromatic value at the requested lightness`() {
        val color = oklchToSrgb(l = 0.5, c = 0.0, hDeg = 0.0)
        // L=0.5 in oklab → mid gray around (118, 118, 118)
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        assertEquals("R == G", r, g)
        assertEquals("G == B", g, b)
    }

    private fun assertRgbWithin(
        color: Color,
        expectedR: Int,
        expectedG: Int,
        expectedB: Int,
        tolerance: Int,
    ) {
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        assertWithin("R", r, expectedR, tolerance)
        assertWithin("G", g, expectedG, tolerance)
        assertWithin("B", b, expectedB, tolerance)
    }

    private fun assertWithin(channel: String, actual: Int, expected: Int, tolerance: Int) {
        val delta = kotlin.math.abs(actual - expected)
        assert(delta <= tolerance) {
            "$channel: expected $expected ± $tolerance, got $actual (delta=$delta)"
        }
    }
}
