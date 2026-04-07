package com.stretchdaily.app.core.engine

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryWeightCalculatorTest {

    private val calculator = CategoryWeightCalculator()

    private fun benchmark(id: String, category: Category) = Benchmark(
        id = id,
        name = id,
        category = category,
        description = "",
        unit = "cm",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = emptyMap(),
    )

    private fun log(benchmarkId: String, tier: FlexibilityTier) = BenchmarkLog(
        benchmarkId = benchmarkId,
        rawValue = "",
        resolvedTier = tier,
        loggedAt = 0L,
    )

    @Test
    fun `no logs returns AVERAGE for every category`() {
        val weights = calculator.calculate(emptyList(), emptyList())

        assertEquals(Category.entries.size, weights.size)
        for (category in Category.entries) {
            assertEquals(FlexibilityTier.AVERAGE.weight, weights.getValue(category), 0.0001)
        }
    }

    @Test
    fun `single benchmark log produces that tier's weight for its category`() {
        val benchmarks = listOf(benchmark("bm-neck", Category.NECK))
        val logs = listOf(log("bm-neck", FlexibilityTier.STIFF))

        val weights = calculator.calculate(logs, benchmarks)

        assertEquals(FlexibilityTier.STIFF.weight, weights.getValue(Category.NECK), 0.0001)
        // Untouched categories still default to AVERAGE.
        assertEquals(FlexibilityTier.AVERAGE.weight, weights.getValue(Category.HIPS), 0.0001)
    }

    @Test
    fun `two benchmarks in same category are averaged`() {
        val benchmarks = listOf(
            benchmark("bm-spine-1", Category.SPINE),
            benchmark("bm-spine-2", Category.SPINE),
        )
        val logs = listOf(
            log("bm-spine-1", FlexibilityTier.STIFF),          // 3.0
            log("bm-spine-2", FlexibilityTier.VERY_FLEXIBLE),  // 1.0
        )

        val weights = calculator.calculate(logs, benchmarks)

        assertEquals(2.0, weights.getValue(Category.SPINE), 0.0001)
    }

    @Test
    fun `every category is present in the result`() {
        val weights = calculator.calculate(emptyList(), emptyList())
        for (category in Category.entries) {
            assertTrue("Missing $category", category in weights)
        }
    }

    @Test
    fun `log referencing unknown benchmark id is ignored`() {
        val weights = calculator.calculate(
            latestLogs = listOf(log("ghost", FlexibilityTier.STIFF)),
            benchmarks = emptyList(),
        )

        // No category should be elevated; everything stays at AVERAGE.
        for (category in Category.entries) {
            assertEquals(FlexibilityTier.AVERAGE.weight, weights.getValue(category), 0.0001)
        }
    }

    @Test
    fun `stiff category has strictly higher weight than flexible category`() {
        val benchmarks = listOf(
            benchmark("bm-neck", Category.NECK),
            benchmark("bm-ankle", Category.ANKLES),
        )
        val logs = listOf(
            log("bm-neck", FlexibilityTier.STIFF),
            log("bm-ankle", FlexibilityTier.VERY_FLEXIBLE),
        )

        val weights = calculator.calculate(logs, benchmarks)

        assertTrue(weights.getValue(Category.NECK) > weights.getValue(Category.ANKLES))
    }
}
