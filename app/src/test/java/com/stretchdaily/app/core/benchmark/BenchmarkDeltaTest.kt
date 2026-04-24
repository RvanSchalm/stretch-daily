package com.stretchdaily.app.core.benchmark

import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkDeltaTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant().toEpochMilli()

    private fun numericLog(date: Long, raw: String): BenchmarkLog = BenchmarkLog(
        benchmarkId = "BM_CERVICAL_ROTATION",
        rawValue = raw,
        resolvedTier = FlexibilityTier.AVERAGE,
        loggedAt = date,
    )

    @Test
    fun `empty logs returns null`() {
        val result = benchmarkDelta(
            logs = emptyList(),
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        assertNull(result)
    }

    @Test
    fun `single log returns null (no baseline to compare)`() {
        val result = benchmarkDelta(
            logs = listOf(numericLog(midday(2026, 4, 10), "70")),
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        assertNull(result)
    }

    @Test
    fun `categorical direction returns null (no delta to show)`() {
        val result = benchmarkDelta(
            logs = listOf(
                numericLog(midday(2025, 10, 1), "LEVEL_2"),
                numericLog(midday(2026, 4, 1), "LEVEL_3"),
            ),
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.CATEGORICAL,
            zoneId = zone,
        )
        assertNull(result)
    }

    @Test
    fun `HIGHER delta is positive when latest exceeds 6mo-prior baseline`() {
        val logs = listOf(
            numericLog(midday(2025, 10, 23), "65"),
            numericLog(midday(2026, 4, 10), "75"),
        )
        val result = benchmarkDelta(
            logs = logs,
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        requireNotNull(result)
        assertEquals(10.0, result.rawDelta, 0.001)
        assertTrue("expected improvement", result.improved)
    }

    @Test
    fun `LOWER delta flips sign - smaller value means improvement`() {
        val logs = listOf(
            numericLog(midday(2025, 10, 23), "15"),
            numericLog(midday(2026, 4, 10), "-5"),
        )
        val result = benchmarkDelta(
            logs = logs,
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.LOWER,
            zoneId = zone,
        )
        requireNotNull(result)
        assertEquals(-20.0, result.rawDelta, 0.001)
        assertTrue("expected improvement (lower is better)", result.improved)
    }

    @Test
    fun `regression flag true when direction disagrees`() {
        val logs = listOf(
            numericLog(midday(2025, 10, 23), "80"),
            numericLog(midday(2026, 4, 10), "72"),
        )
        val result = benchmarkDelta(
            logs = logs,
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        requireNotNull(result)
        assertEquals(-8.0, result.rawDelta, 0.001)
        assertTrue("expected regression (HIGHER benchmark, delta negative)", !result.improved)
    }

    @Test
    fun `nearest-date fallback picks closest log to 6mo anchor`() {
        val logs = listOf(
            numericLog(midday(2024, 1, 2), "50"),
            numericLog(midday(2025, 11, 5), "65"),
            numericLog(midday(2026, 4, 10), "70"),
        )
        val result = benchmarkDelta(
            logs = logs,
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        requireNotNull(result)
        assertEquals(5.0, result.rawDelta, 0.001)
    }

    @Test
    fun `non-parseable rawValue on baseline returns null`() {
        val logs = listOf(
            numericLog(midday(2025, 10, 23), "LEVEL_3"),
            numericLog(midday(2026, 4, 10), "75"),
        )
        val result = benchmarkDelta(
            logs = logs,
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        assertNull(result)
    }
}
