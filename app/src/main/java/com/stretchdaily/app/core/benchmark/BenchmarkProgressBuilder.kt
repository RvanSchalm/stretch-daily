package com.stretchdaily.app.core.benchmark

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * One plotted point on the benchmark progress chart.
 *
 * - [xRatio] is `0..1` across the data's time span (oldest → newest log).
 *   Empty/single-point series special-case below.
 * - [rawValue] is the raw numeric reading (degrees, cm, etc.) for numeric
 *   benchmarks, or `null` for the one categorical benchmark (ATG Split
 *   Squat). For categorical, [tier] is the y-axis source.
 */
data class ProgressPoint(
    val xRatio: Float,
    val rawValue: Float?,
    val tier: FlexibilityTier,
    val timestampMillis: Long,
)

/**
 * Series of [ProgressPoint]s plus chart range metadata. Empty when there
 * are no logs.
 *
 * For numeric benchmarks: [rawMin] and [rawMax] frame the Y axis with 5%
 * padding around the data range. For single-point series, padding is 5%
 * of the value (or ±0.5 if the value is 0).
 *
 * [yTickLabels] are pre-formatted to 1 decimal place — `[max, mid, min]`
 * top-to-bottom on the Y gutter.
 *
 * [firstMonth] and [lastMonth] anchor the X axis at month granularity for
 * the scrollable BigChart's month-label row.
 */
data class ProgressSeries(
    val points: List<ProgressPoint>,
    val isCategorical: Boolean,
    val rawMin: Float,
    val rawMax: Float,
    val yTickLabels: List<String>,
    val firstMonth: YearMonth,
    val lastMonth: YearMonth,
) {
    companion object {
        val EMPTY: ProgressSeries = ProgressSeries(
            points = emptyList(),
            isCategorical = false,
            rawMin = 0f,
            rawMax = 0f,
            yTickLabels = emptyList(),
            firstMonth = YearMonth.of(2000, 1),
            lastMonth = YearMonth.of(2000, 1),
        )
    }
}

/**
 * Pure-Kotlin builder. Lives in `core/` so unit tests don't need Compose.
 *
 * @param benchmark drives the categorical-vs-numeric branch and surfaces
 *        the unit string indirectly via the [yTickLabels] formatting.
 */
internal object BenchmarkProgressBuilder {

    private val ZONE: ZoneId = ZoneId.systemDefault()

    fun build(benchmark: Benchmark, logs: List<BenchmarkLog>): ProgressSeries {
        if (logs.isEmpty()) return ProgressSeries.EMPTY

        val sorted = logs.sortedBy { it.loggedAt }
        val isCategorical = benchmark.inputType == BenchmarkInputType.CATEGORICAL

        // Build points first.
        val points = if (sorted.size == 1) {
            val only = sorted.first()
            listOf(
                ProgressPoint(
                    xRatio = 0.5f,
                    rawValue = if (isCategorical) null else only.rawValue.toFloatOrNull(),
                    tier = only.resolvedTier,
                    timestampMillis = only.loggedAt,
                )
            )
        } else {
            val minT = sorted.first().loggedAt
            val maxT = sorted.last().loggedAt
            val span = (maxT - minT).coerceAtLeast(1L)
            sorted.map { log ->
                ProgressPoint(
                    xRatio = (log.loggedAt - minT).toFloat() / span.toFloat(),
                    rawValue = if (isCategorical) null else log.rawValue.toFloatOrNull(),
                    tier = log.resolvedTier,
                    timestampMillis = log.loggedAt,
                )
            }
        }

        // Y-axis range + tick labels.
        val (rawMin, rawMax, ticks) = if (isCategorical) {
            // Categorical: tier-based Y, no raw values; ticks are tier names.
            Triple(
                0f,
                1f,
                listOf("VERY FLEXIBLE", "AVERAGE", "STIFF"),
            )
        } else {
            val rawValues = points.mapNotNull { it.rawValue }
            if (rawValues.isEmpty()) {
                Triple(0f, 0f, emptyList())
            } else {
                val dataMin = rawValues.min()
                val dataMax = rawValues.max()
                val (minPad, maxPad) = if (dataMin == dataMax) {
                    val pad = if (dataMin == 0f) 0.5f else dataMin * 0.05f
                    (dataMin - pad) to (dataMax + pad)
                } else {
                    val span = dataMax - dataMin
                    val pad = span * 0.05f
                    (dataMin - pad) to (dataMax + pad)
                }
                val mid = (minPad + maxPad) / 2f
                Triple(
                    minPad,
                    maxPad,
                    listOf(
                        "%.1f".format(maxPad),
                        "%.1f".format(mid),
                        "%.1f".format(minPad),
                    ),
                )
            }
        }

        // X-axis month bounds.
        val firstMonth = YearMonth.from(Instant.ofEpochMilli(sorted.first().loggedAt).atZone(ZONE))
        val nowMonth = YearMonth.now(ZONE)
        val lastLogMonth = YearMonth.from(Instant.ofEpochMilli(sorted.last().loggedAt).atZone(ZONE))
        val lastMonth = if (nowMonth.isAfter(lastLogMonth)) nowMonth else lastLogMonth

        return ProgressSeries(
            points = points,
            isCategorical = isCategorical,
            rawMin = rawMin,
            rawMax = rawMax,
            yTickLabels = ticks,
            firstMonth = firstMonth,
            lastMonth = lastMonth,
        )
    }
}
