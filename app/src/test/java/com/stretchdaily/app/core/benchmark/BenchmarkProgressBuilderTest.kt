package com.stretchdaily.app.core.benchmark

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests for [BenchmarkProgressBuilder]. Exercises numeric +
 * categorical inputs, padding/formatting of Y range, and the X-axis
 * month bounds.
 */
class BenchmarkProgressBuilderTest {

    @Test
    fun `empty logs produce empty series`() {
        val series = BenchmarkProgressBuilder.build(numericBenchmark(), emptyList())
        assertTrue(series.points.isEmpty())
        assertEquals(0f, series.rawMin, 0.0001f)
        assertEquals(0f, series.rawMax, 0.0001f)
    }

    @Test
    fun `single numeric log centers x and pads y range by 5 percent of value`() {
        val log = numericLog(value = 60f, daysAgo = 30)
        val series = BenchmarkProgressBuilder.build(numericBenchmark(), listOf(log))

        assertEquals(1, series.points.size)
        val point = series.points[0]
        assertEquals(0.5f, point.xRatio, 0.0001f)
        assertEquals(60f, point.rawValue!!, 0.0001f)
        // Single-point fallback: ±5% of value.
        assertEquals(57f, series.rawMin, 0.0001f)
        assertEquals(63f, series.rawMax, 0.0001f)
    }

    @Test
    fun `multi-log numeric is sorted oldest first and frames Y at min and max with padding`() {
        // Three logs out of order. After sorting: 50 → 65 → 75.
        // Range 25, padding 1.25 → rawMin = 48.75, rawMax = 76.25.
        val logs = listOf(
            numericLog(value = 50f, daysAgo = 90),
            numericLog(value = 75f, daysAgo = 30),
            numericLog(value = 65f, daysAgo = 60),
        )
        val series = BenchmarkProgressBuilder.build(numericBenchmark(), logs)

        assertEquals(listOf(50f, 65f, 75f), series.points.map { it.rawValue })
        assertEquals(48.75f, series.rawMin, 0.001f)
        assertEquals(76.25f, series.rawMax, 0.001f)
    }

    @Test
    fun `multi-log Y tick labels render max mid min to one decimal`() {
        val logs = listOf(
            numericLog(value = 50f, daysAgo = 60),
            numericLog(value = 75f, daysAgo = 30),
        )
        val series = BenchmarkProgressBuilder.build(numericBenchmark(), logs)

        // Padded range: 48.75 .. 76.25; mid = 62.5. Format to 1 dp.
        assertEquals(listOf("76.3", "62.5", "48.8"), series.yTickLabels)
    }

    @Test
    fun `categorical benchmark produces tier-based series with null raw`() {
        val log = categoricalLog(tier = FlexibilityTier.AVERAGE, daysAgo = 30)
        val series = BenchmarkProgressBuilder.build(categoricalBenchmark(), listOf(log))

        assertTrue(series.isCategorical)
        assertNotNull(series.points.firstOrNull())
        val point = series.points[0]
        assertNull(point.rawValue)
        assertEquals(FlexibilityTier.AVERAGE, point.tier)
        // Categorical Y-tick labels are tier names, fixed.
        assertEquals(listOf("VERY FLEXIBLE", "AVERAGE", "STIFF"), series.yTickLabels)
    }

    @Test
    fun `descending direction benchmark stores raw values unmodified`() {
        // BM_APLEY_SCRATCH = LOWER per benchmarkBetter() — lower cm gap = better.
        // The builder plots raw values as-is regardless of direction.
        val benchmark = numericBenchmark(id = "BM_APLEY_SCRATCH")
        val logs = listOf(
            numericLog(value = 14f, daysAgo = 90, benchmarkId = "BM_APLEY_SCRATCH"),
            numericLog(value = 8f,  daysAgo = 30, benchmarkId = "BM_APLEY_SCRATCH"),
        )
        val series = BenchmarkProgressBuilder.build(benchmark, logs)

        assertEquals(listOf(14f, 8f), series.points.map { it.rawValue })
        // Range 6, padding 0.3 → rawMin = 7.7, rawMax = 14.3.
        assertEquals(7.7f, series.rawMin, 0.001f)
        assertEquals(14.3f, series.rawMax, 0.001f)
    }

    @Test
    fun `firstMonth and lastMonth bound the data range`() {
        val logs = listOf(
            numericLog(value = 50f, daysAgo = 70),
            numericLog(value = 75f, daysAgo = 5),
        )
        val series = BenchmarkProgressBuilder.build(numericBenchmark(), logs)
        // Both months are real YearMonth objects; lastMonth must not precede firstMonth.
        assertTrue(!series.lastMonth.isBefore(series.firstMonth))
    }

    @Test
    fun `single log Y range with zero value falls back to plus minus half`() {
        val log = numericLog(value = 0f, daysAgo = 30)
        val series = BenchmarkProgressBuilder.build(numericBenchmark(), listOf(log))
        assertEquals(-0.5f, series.rawMin, 0.0001f)
        assertEquals(0.5f, series.rawMax, 0.0001f)
    }

    // ── helpers ──────────────────────────────────────────────

    private fun numericBenchmark(id: String = "BM_CERVICAL_ROTATION"): Benchmark = Benchmark(
        id = id,
        name = id,
        category = Category.NECK,
        description = "",
        unit = "°",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = mapOf(
            FlexibilityTier.STIFF.name to "≤ 50°",
            FlexibilityTier.BELOW_AVERAGE.name to "50–60°",
            FlexibilityTier.AVERAGE.name to "60–70°",
            FlexibilityTier.FLEXIBLE.name to "70–80°",
            FlexibilityTier.VERY_FLEXIBLE.name to "≥ 80°",
        ),
    )

    private fun categoricalBenchmark(): Benchmark = Benchmark(
        id = "BM_ATG_SPLIT_SQUAT",
        name = "ATG Split Squat",
        category = Category.HIPS,
        description = "",
        unit = "",
        inputType = BenchmarkInputType.CATEGORICAL,
        tierRanges = emptyMap(),
    )

    private fun numericLog(
        value: Float,
        daysAgo: Int,
        benchmarkId: String = "BM_CERVICAL_ROTATION",
    ): BenchmarkLog = BenchmarkLog(
        id = 0L,
        benchmarkId = benchmarkId,
        rawValue = value.toString(),
        resolvedTier = FlexibilityTier.AVERAGE,
        loggedAt = System.currentTimeMillis() - daysAgo * 86_400_000L,
    )

    private fun categoricalLog(
        tier: FlexibilityTier,
        daysAgo: Int,
    ): BenchmarkLog = BenchmarkLog(
        id = 0L,
        benchmarkId = "BM_ATG_SPLIT_SQUAT",
        rawValue = tier.name,
        resolvedTier = tier,
        loggedAt = System.currentTimeMillis() - daysAgo * 86_400_000L,
    )
}
