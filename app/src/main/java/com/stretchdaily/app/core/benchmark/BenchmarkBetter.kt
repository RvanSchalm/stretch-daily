package com.stretchdaily.app.core.benchmark

/**
 * Whether a higher reading on a benchmark indicates more flexibility
 * ([HIGHER]) or less ([LOWER]), or whether the benchmark is qualitative
 * and has no numeric direction ([CATEGORICAL]).
 *
 * Duplicates [TierResolver]'s private `Direction` field intentionally —
 * the resolver is scoped for numeric → tier math and keeping that enum
 * private lets us change its internals freely. Analytics and (later)
 * Library stats only need the direction, not the breakpoints, so they
 * read from this sibling lookup.
 */
enum class Better { HIGHER, LOWER, CATEGORICAL }

/**
 * Pure lookup: direction for a benchmark id. Unknown ids fall back to
 * [Better.CATEGORICAL] — callers interpret that as "don't draw a delta
 * chip" rather than throwing, which keeps Analytics resilient to future
 * benchmark seed changes.
 */
fun benchmarkBetter(benchmarkId: String): Better = when (benchmarkId) {
    "BM_CERVICAL_ROTATION",
    "BM_THORACIC_ROTATION",
    "BM_KNEE_TO_WALL",
    "BM_WRIST_EXTENSION",
    "BM_WRIST_FLEXION" -> Better.HIGHER

    "BM_APLEY_SCRATCH",
    "BM_BUTTERFLY",
    "BM_SIT_AND_REACH",
    "BM_THOMAS_TEST" -> Better.LOWER

    else -> Better.CATEGORICAL
}
