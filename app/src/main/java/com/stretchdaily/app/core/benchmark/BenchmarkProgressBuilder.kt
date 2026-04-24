package com.stretchdaily.app.core.benchmark

import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier

/**
 * One plotted point on the benchmark progress chart. The X/Y values are
 * normalized to `0..1` so the Compose Canvas can multiply them by the
 * available size without doing any math itself. [yRatio] is laid out so
 * `0f` is the top of the chart (most flexible) and `1f` is the bottom
 * (stiffest) — improvement therefore reads as a line that trends upward.
 */
data class ProgressPoint(
    val xRatio: Float,
    val yRatio: Float,
    val timestampMillis: Long,
    val tier: FlexibilityTier,
)

/** Series of [ProgressPoint]s ready to render. Empty when there are no logs. */
data class ProgressSeries(val points: List<ProgressPoint>)

/**
 * Pure-Kotlin builder that turns a list of [BenchmarkLog]s into a
 * [ProgressSeries] with normalized chart coordinates. Lives in `core/` so
 * it can be unit tested on the JVM without Compose or Room.
 *
 * - Empty input → empty series.
 * - Single point → centered horizontally so it's visible regardless of
 *   chart width.
 * - Multiple points → X is interpolated linearly across the time span
 *   between the oldest and newest log.
 *
 * The Y axis is the resolved tier rather than the raw numeric value: that
 * keeps the chart uniform across the 9 numeric and 1 categorical
 * benchmarks, and matches what the Longevity Engine actually consumes.
 */
internal object BenchmarkProgressBuilder {

    fun build(logs: List<BenchmarkLog>): ProgressSeries {
        if (logs.isEmpty()) return ProgressSeries(emptyList())
        val sorted = logs.sortedBy { it.loggedAt }
        if (sorted.size == 1) {
            val only = sorted.first()
            return ProgressSeries(
                listOf(
                    ProgressPoint(
                        xRatio = 0.5f,
                        yRatio = tierToY(only.resolvedTier),
                        timestampMillis = only.loggedAt,
                        tier = only.resolvedTier,
                    )
                )
            )
        }
        val minT = sorted.first().loggedAt
        val maxT = sorted.last().loggedAt
        val span = (maxT - minT).coerceAtLeast(1L)
        return ProgressSeries(
            sorted.map { log ->
                ProgressPoint(
                    xRatio = (log.loggedAt - minT).toFloat() / span.toFloat(),
                    yRatio = tierToY(log.resolvedTier),
                    timestampMillis = log.loggedAt,
                    tier = log.resolvedTier,
                )
            }
        )
    }

    /**
     * Maps a tier to the vertical center of its band in a 5-row chart.
     * VERY_FLEXIBLE → 0.1 (top), STIFF → 0.9 (bottom).
     */
    internal fun tierToY(tier: FlexibilityTier): Float {
        val index = when (tier) {
            FlexibilityTier.VERY_FLEXIBLE -> 0
            FlexibilityTier.FLEXIBLE -> 1
            FlexibilityTier.AVERAGE -> 2
            FlexibilityTier.BELOW_AVERAGE -> 3
            FlexibilityTier.STIFF -> 4
        }
        return (index + 0.5f) / 5f
    }
}
