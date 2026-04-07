package com.stretchdaily.app.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM tests for the benchmarks-due banner logic. */
class BenchmarkRepositoryDueTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `no logs at all is due`() {
        assertTrue(
            BenchmarkRepository.computeBenchmarksDue(
                lastLoggedAt = null,
                now = midday(2026, 4, 15),
                zoneId = zone,
            )
        )
    }

    @Test
    fun `log within 30 days is not due on a normal day`() {
        assertFalse(
            BenchmarkRepository.computeBenchmarksDue(
                lastLoggedAt = midday(2026, 4, 1),
                now = midday(2026, 4, 15),
                zoneId = zone,
            )
        )
    }

    @Test
    fun `log exactly 31 days ago is due`() {
        assertTrue(
            BenchmarkRepository.computeBenchmarksDue(
                lastLoggedAt = midday(2026, 3, 15),
                now = midday(2026, 4, 15),
                zoneId = zone,
            )
        )
    }

    @Test
    fun `log exactly 30 days ago is still fresh`() {
        assertFalse(
            BenchmarkRepository.computeBenchmarksDue(
                lastLoggedAt = midday(2026, 3, 16),
                now = midday(2026, 4, 15),
                zoneId = zone,
            )
        )
    }

    @Test
    fun `first of month with yesterday log is due`() {
        // Log yesterday, today is 1st of month -> nudge.
        assertTrue(
            BenchmarkRepository.computeBenchmarksDue(
                lastLoggedAt = midday(2026, 4, 30),
                now = midday(2026, 5, 1),
                zoneId = zone,
            )
        )
    }

    @Test
    fun `first of month after logging today is not due`() {
        // User already re-logged today on the 1st.
        assertFalse(
            BenchmarkRepository.computeBenchmarksDue(
                lastLoggedAt = midday(2026, 5, 1),
                now = midday(2026, 5, 1),
                zoneId = zone,
            )
        )
    }
}
