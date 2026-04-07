package com.stretchdaily.app.core.engine

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import javax.inject.Inject

/**
 * Turns the user's most recent benchmark logs into a per-[Category] weight that
 * the [SessionBuilder] feeds into its weighted-random selection.
 *
 * Algorithm:
 * - Join each log to its benchmark to find the category.
 * - Group resolved tiers by category.
 * - For each of the seven categories: average the tier weights of the logs
 *   that landed in that category. Categories with no logs default to
 *   [FlexibilityTier.AVERAGE]'s weight (2.0).
 *
 * Higher weight = stiffer = more session slots. Spine, Hips, and Wrists each
 * have two benchmarks; the average of their two tiers is used.
 */
class CategoryWeightCalculator @Inject constructor() {

    fun calculate(
        latestLogs: List<BenchmarkLog>,
        benchmarks: List<Benchmark>,
    ): Map<Category, Double> {
        val benchmarkById = benchmarks.associateBy { it.id }
        val tiersByCategory = mutableMapOf<Category, MutableList<FlexibilityTier>>()

        for (log in latestLogs) {
            val category = benchmarkById[log.benchmarkId]?.category ?: continue
            tiersByCategory.getOrPut(category) { mutableListOf() }.add(log.resolvedTier)
        }

        return Category.entries.associateWith { category ->
            val tiers = tiersByCategory[category]
            if (tiers.isNullOrEmpty()) {
                FlexibilityTier.AVERAGE.weight
            } else {
                tiers.map { it.weight }.average()
            }
        }
    }
}
