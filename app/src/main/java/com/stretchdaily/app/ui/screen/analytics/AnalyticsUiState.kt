package com.stretchdaily.app.ui.screen.analytics

import com.stretchdaily.app.core.benchmark.BenchmarkDelta
import com.stretchdaily.app.core.benchmark.ProgressSeries
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.Category

/**
 * Per-benchmark row on the Analytics tab. [series] is the full log
 * history fed into [com.stretchdaily.app.ui.components.BigChart].
 * [delta] is null when there aren't enough logs to compute one;
 * [latestRawValue] null = no logs at all and the card renders the
 * empty-chart state with no right-side value.
 */
data class BenchmarkAnalyticsCardState(
    val benchmark: Benchmark,
    val series: ProgressSeries,
    val latestRawValue: String?,
    val delta: BenchmarkDelta?,
)

/**
 * Screen-wide UI state. [allCategories] is the deduped set of
 * categories represented in the catalog, order-preserved; the "All"
 * pill is rendered by the screen layer, not baked in here.
 * [filter] = null ⇒ "All".
 */
data class AnalyticsUiState(
    val cards: List<BenchmarkAnalyticsCardState> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val filter: Category? = null,
) {
    val visibleCards: List<BenchmarkAnalyticsCardState>
        get() = filter?.let { c -> cards.filter { it.benchmark.category == c } } ?: cards
}
