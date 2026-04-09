package com.stretchdaily.app.core.benchmark

import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for [BenchmarkProgressBuilder] — the helper that turns a
 * list of logs into normalized chart coordinates for [BenchmarkProgressChart].
 */
class BenchmarkProgressBuilderTest {

    private fun log(id: Long, loggedAt: Long, tier: FlexibilityTier): BenchmarkLog =
        BenchmarkLog(
            id = id,
            benchmarkId = "BM_KNEE_TO_WALL",
            rawValue = "0",
            resolvedTier = tier,
            loggedAt = loggedAt,
        )

    @Test
    fun `empty input yields empty series`() {
        val series = BenchmarkProgressBuilder.build(emptyList())
        assertTrue(series.points.isEmpty())
    }

    @Test
    fun `single log is centered horizontally`() {
        val series = BenchmarkProgressBuilder.build(
            listOf(log(id = 1, loggedAt = 1_700_000_000_000L, tier = FlexibilityTier.AVERAGE))
        )
        assertEquals(1, series.points.size)
        val only = series.points.single()
        assertEquals(0.5f, only.xRatio, 0.0001f)
        assertEquals(BenchmarkProgressBuilder.tierToY(FlexibilityTier.AVERAGE), only.yRatio, 0.0001f)
        assertEquals(FlexibilityTier.AVERAGE, only.tier)
        assertEquals(1_700_000_000_000L, only.timestampMillis)
    }

    @Test
    fun `multiple logs span the full chart width`() {
        val logs = listOf(
            log(id = 1, loggedAt = 1_700_000_000_000L, tier = FlexibilityTier.STIFF),
            log(id = 2, loggedAt = 1_700_500_000_000L, tier = FlexibilityTier.AVERAGE),
            log(id = 3, loggedAt = 1_701_000_000_000L, tier = FlexibilityTier.FLEXIBLE),
        )
        val series = BenchmarkProgressBuilder.build(logs)

        assertEquals(3, series.points.size)
        // First point pinned to left edge, last to right edge.
        assertEquals(0f, series.points.first().xRatio, 0.0001f)
        assertEquals(1f, series.points.last().xRatio, 0.0001f)
        // Equal spacing → middle point should sit at 0.5.
        assertEquals(0.5f, series.points[1].xRatio, 0.0001f)
    }

    @Test
    fun `unsorted input is sorted oldest first`() {
        val logs = listOf(
            log(id = 1, loggedAt = 3_000L, tier = FlexibilityTier.STIFF),
            log(id = 2, loggedAt = 1_000L, tier = FlexibilityTier.AVERAGE),
            log(id = 3, loggedAt = 2_000L, tier = FlexibilityTier.FLEXIBLE),
        )
        val series = BenchmarkProgressBuilder.build(logs)

        assertEquals(listOf(1_000L, 2_000L, 3_000L), series.points.map { it.timestampMillis })
        assertEquals(
            listOf(FlexibilityTier.AVERAGE, FlexibilityTier.FLEXIBLE, FlexibilityTier.STIFF),
            series.points.map { it.tier },
        )
    }

    @Test
    fun `tierToY places very flexible at the top and stiff at the bottom`() {
        val veryFlexY = BenchmarkProgressBuilder.tierToY(FlexibilityTier.VERY_FLEXIBLE)
        val flexY = BenchmarkProgressBuilder.tierToY(FlexibilityTier.FLEXIBLE)
        val avgY = BenchmarkProgressBuilder.tierToY(FlexibilityTier.AVERAGE)
        val belowY = BenchmarkProgressBuilder.tierToY(FlexibilityTier.BELOW_AVERAGE)
        val stiffY = BenchmarkProgressBuilder.tierToY(FlexibilityTier.STIFF)

        // Strictly increasing — better tiers higher (smaller Y).
        assertTrue(veryFlexY < flexY)
        assertTrue(flexY < avgY)
        assertTrue(avgY < belowY)
        assertTrue(belowY < stiffY)
        // Bands are evenly distributed, centered at (i + 0.5) / 5.
        assertEquals(0.1f, veryFlexY, 0.0001f)
        assertEquals(0.9f, stiffY, 0.0001f)
        assertEquals(0.5f, avgY, 0.0001f)
    }

    @Test
    fun `simultaneous logs collapse to the same x`() {
        // Edge case: two logs with identical timestamps. Span is coerced to 1
        // ms so we don't divide by zero, and both points sit at xRatio = 0.
        val timestamp = 1_700_000_000_000L
        val series = BenchmarkProgressBuilder.build(
            listOf(
                log(id = 1, loggedAt = timestamp, tier = FlexibilityTier.STIFF),
                log(id = 2, loggedAt = timestamp, tier = FlexibilityTier.AVERAGE),
            )
        )

        assertEquals(2, series.points.size)
        // Both points end up at the same x (left edge) since the span is 0.
        assertEquals(series.points[0].xRatio, series.points[1].xRatio, 0.0001f)
    }
}
