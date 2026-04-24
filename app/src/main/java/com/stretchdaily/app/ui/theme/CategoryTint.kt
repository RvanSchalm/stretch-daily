package com.stretchdaily.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.stretchdaily.app.core.model.Category

/**
 * Per-[Category] tint colors for striped exercise tiles, session progress
 * bars, and benchmark category-strip headers.
 *
 * Authored as oklch triples (spec §4.3 + handoff README "Category tints"):
 * all share `L = 0.82`, `C = 0.04`; only hue varies so tints read as
 * "quiet colored paper" rather than saturated category labels.
 *
 * Compose has no native oklch. Each tint is resolved to sRGB once via
 * [oklchToSrgb] and memoized in the `tints` lazy map — one allocation per
 * tint per process lifetime.
 */
private data class OklchTriple(val l: Double, val c: Double, val h: Double)

private val oklchByCategory: Map<Category, OklchTriple> = mapOf(
    Category.NECK      to OklchTriple(0.82, 0.04, 140.0),
    Category.SHOULDERS to OklchTriple(0.82, 0.04, 110.0),
    Category.WRISTS    to OklchTriple(0.82, 0.04, 80.0),
    Category.SPINE     to OklchTriple(0.82, 0.04, 170.0),
    Category.HIPS      to OklchTriple(0.82, 0.04, 50.0),
    Category.KNEES     to OklchTriple(0.82, 0.04, 200.0),
    Category.ANKLES    to OklchTriple(0.82, 0.04, 25.0),
)

/**
 * Lazy memoized tint map. First call computes all 7; subsequent calls are
 * O(1) hash lookups. Resolved at the process level, not per-Composition.
 */
private val tints: Map<Category, Color> by lazy {
    oklchByCategory.mapValues { (_, oklch) ->
        oklchToSrgb(l = oklch.l, c = oklch.c, hDeg = oklch.h)
    }
}

/** Get the Sage tint for this category. Cheap after first call (memoized). */
fun Category.tint(): Color =
    tints.getValue(this) // getValue throws NSEE if missing — enforces enum/map parity.
