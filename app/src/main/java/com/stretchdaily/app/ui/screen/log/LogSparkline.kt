package com.stretchdaily.app.ui.screen.log

import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier

private const val SIX_MONTHS_MILLIS = 6L * 30L * 24L * 60L * 60L * 1000L

/**
 * Builds a list of 0..1 sparkline values for the last six months of [logs],
 * relative to [nowMillis]. Each value encodes the log's resolved tier:
 *
 *  - STIFF → 0.0   (bottom of the sparkline, worst)
 *  - BELOW_AVERAGE → 0.25
 *  - AVERAGE → 0.5
 *  - FLEXIBLE → 0.75
 *  - VERY_FLEXIBLE → 1.0 (top of the sparkline, best)
 *
 * Empty when there are no in-window logs — the `Sparkline` primitive renders
 * a dashed flat line in that case (see R2 `Sparkline.kt`).
 *
 * Kept as a top-level function (not a companion of `BenchmarkProgressBuilder`)
 * so its "higher value is better" semantic stays decoupled from the inverted-y
 * coordinate system `BigChart` requires.
 */
internal fun sparklineValues(
    logs: List<BenchmarkLog>,
    nowMillis: Long,
): List<Double> {
    val cutoff = nowMillis - SIX_MONTHS_MILLIS
    return logs
        .asSequence()
        .filter { it.loggedAt >= cutoff }
        .sortedBy { it.loggedAt }
        .map { tierToValue(it.resolvedTier) }
        .toList()
}

private fun tierToValue(tier: FlexibilityTier): Double = when (tier) {
    FlexibilityTier.STIFF -> 0.0
    FlexibilityTier.BELOW_AVERAGE -> 0.25
    FlexibilityTier.AVERAGE -> 0.5
    FlexibilityTier.FLEXIBLE -> 0.75
    FlexibilityTier.VERY_FLEXIBLE -> 1.0
}
