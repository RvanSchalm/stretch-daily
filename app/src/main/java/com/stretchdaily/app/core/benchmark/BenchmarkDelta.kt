package com.stretchdaily.app.core.benchmark

import com.stretchdaily.app.core.model.BenchmarkLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Numeric delta between a benchmark's latest log and the log closest to
 * 6 months prior. `null` when:
 *  - fewer than 2 logs exist;
 *  - [better] is [Better.CATEGORICAL] (nothing to plot numerically);
 *  - either end's [BenchmarkLog.rawValue] is not parseable as a number.
 *
 * [improved] is true when the direction of change matches [better]:
 * ascending benchmarks (HIGHER) want a positive delta, descending
 * (LOWER) want a negative one.
 */
data class BenchmarkDelta(
    val rawDelta: Double,
    val improved: Boolean,
)

fun benchmarkDelta(
    logs: List<BenchmarkLog>,
    nowEpochMillis: Long,
    better: Better,
    zoneId: ZoneId = ZoneId.systemDefault(),
): BenchmarkDelta? {
    if (better == Better.CATEGORICAL) return null
    if (logs.size < 2) return null

    val sorted = logs.sortedBy { it.loggedAt }
    val latest = sorted.last()

    val today = LocalDate.ofInstant(Instant.ofEpochMilli(nowEpochMillis), zoneId)
    val anchor = today.minusMonths(6)

    val candidates = sorted.dropLast(1)
    if (candidates.isEmpty()) return null
    val baseline = candidates.minBy { log ->
        val logDate = LocalDate.ofInstant(Instant.ofEpochMilli(log.loggedAt), zoneId)
        abs(ChronoUnit.DAYS.between(anchor, logDate))
    }

    val latestValue = latest.rawValue.parseNumericOrNull() ?: return null
    val baselineValue = baseline.rawValue.parseNumericOrNull() ?: return null

    val delta = latestValue - baselineValue
    val improved = when (better) {
        Better.HIGHER -> delta > 0
        Better.LOWER -> delta < 0
        Better.CATEGORICAL -> false
    }
    return BenchmarkDelta(rawDelta = delta, improved = improved)
}

private fun String.parseNumericOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()
