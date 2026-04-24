package com.stretchdaily.app.data

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM tests for the two companion helpers that feed
 * [BenchmarkRepository.overdueFlow] and [BenchmarkRepository.nextDueFlow].
 * The helpers are extracted from the repository so they can be exercised
 * without Room — same pattern as [SessionRepository.computeStreak] and
 * [BenchmarkRepository.computeBenchmarksDue].
 */
class BenchmarkRepositoryDueSelectionTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant().toEpochMilli()

    private fun bm(id: String, category: Category = Category.HIPS): Benchmark =
        Benchmark(
            id = id,
            name = id,
            category = category,
            description = "",
            unit = "",
            inputType = BenchmarkInputType.NUMERIC,
            tierRanges = emptyMap(),
        )

    private fun log(benchmarkId: String, loggedAt: Long): BenchmarkLog =
        BenchmarkLog(
            id = 0,
            benchmarkId = benchmarkId,
            rawValue = "0",
            resolvedTier = FlexibilityTier.AVERAGE,
            loggedAt = loggedAt,
        )

    @Test
    fun `computeOverdueBenchmarks returns all when no logs exist`() {
        val benchmarks = listOf(bm("A"), bm("B"), bm("C"))
        val overdue = BenchmarkRepository.computeOverdueBenchmarks(
            benchmarks = benchmarks,
            latestLogs = emptyList(),
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals(benchmarks, overdue)
    }

    @Test
    fun `computeOverdueBenchmarks excludes benchmarks logged within current month`() {
        val benchmarks = listOf(bm("A"), bm("B"))
        val latestLogs = listOf(log("A", midday(2026, 4, 10)))
        val overdue = BenchmarkRepository.computeOverdueBenchmarks(
            benchmarks = benchmarks,
            latestLogs = latestLogs,
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals(listOf(bm("B")), overdue)
    }

    @Test
    fun `computeOverdueBenchmarks flags benchmarks whose last log is before the 1st of this month`() {
        val benchmarks = listOf(bm("A"))
        val latestLogs = listOf(log("A", midday(2026, 3, 28)))
        val overdue = BenchmarkRepository.computeOverdueBenchmarks(
            benchmarks = benchmarks,
            latestLogs = latestLogs,
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals(listOf(bm("A")), overdue)
    }

    @Test
    fun `computeNextDue returns null when catalog is empty`() {
        val next = BenchmarkRepository.computeNextDue(
            benchmarks = emptyList(),
            latestLogs = emptyList(),
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertNull(next)
    }

    @Test
    fun `computeNextDue returns 0 days for a never-logged benchmark`() {
        val benchmarks = listOf(bm("A"), bm("B"))
        val next = BenchmarkRepository.computeNextDue(
            benchmarks = benchmarks,
            latestLogs = emptyList(),
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        // Both are never-logged — tie-break goes to the first id.
        assertEquals("A", next?.benchmark?.id)
        assertEquals(0, next?.daysUntilDue)
    }

    @Test
    fun `computeNextDue returns forward-looking days for recently-logged benchmarks`() {
        // Logged today (4/23); next due = +30 = 5/23; daysUntil = 30.
        val benchmarks = listOf(bm("A"))
        val latestLogs = listOf(log("A", midday(2026, 4, 23)))
        val next = BenchmarkRepository.computeNextDue(
            benchmarks = benchmarks,
            latestLogs = latestLogs,
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals("A", next?.benchmark?.id)
        assertEquals(30, next?.daysUntilDue)
    }

    @Test
    fun `computeNextDue picks the benchmark with the nearest due date`() {
        // A logged 4/20 → due 5/20 → 27 days out
        // B logged 4/10 → due 5/10 → 17 days out → picked
        val benchmarks = listOf(bm("A"), bm("B"))
        val latestLogs = listOf(
            log("A", midday(2026, 4, 20)),
            log("B", midday(2026, 4, 10)),
        )
        val next = BenchmarkRepository.computeNextDue(
            benchmarks = benchmarks,
            latestLogs = latestLogs,
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals("B", next?.benchmark?.id)
        assertEquals(17, next?.daysUntilDue)
    }

    @Test
    fun `computeNextDue clamps overdue benchmarks to 0 days`() {
        // A logged on 2026-01-01 → due 1/31 → negative; clamp to 0 overdue.
        val benchmarks = listOf(bm("A"), bm("B"))
        val latestLogs = listOf(
            log("A", midday(2026, 1, 1)),
            log("B", midday(2026, 4, 23)),
        )
        val next = BenchmarkRepository.computeNextDue(
            benchmarks = benchmarks,
            latestLogs = latestLogs,
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals("A", next?.benchmark?.id)
        assertEquals(0, next?.daysUntilDue)
    }
}
