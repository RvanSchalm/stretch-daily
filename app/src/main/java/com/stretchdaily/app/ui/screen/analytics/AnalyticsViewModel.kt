package com.stretchdaily.app.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.benchmark.BenchmarkProgressBuilder
import com.stretchdaily.app.core.benchmark.ProgressSeries
import com.stretchdaily.app.core.benchmark.benchmarkBetter
import com.stretchdaily.app.core.benchmark.benchmarkDelta
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Backs the Progress / Analytics tab. Emits one
 * [BenchmarkAnalyticsCardState] per benchmark in the catalog, each
 * carrying a full [ProgressSeries] for the [com.stretchdaily.app.ui.components.BigChart]
 * and an optional 6-month delta for the right-hand chip.
 *
 * The fan-out pattern is the same one used by R5's `BenchmarkLogViewModel`
 * — [BenchmarkRepository.observeAllBenchmarks] maps to a parallel list of
 * `observeLogsFor(id)` flows combined with `combine(*flows)`. `flatMapLatest`
 * ensures that if the catalog itself changes (rare, only on Import +
 * delete-all + re-seed) the subscription re-fans.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val benchmarkRepository: BenchmarkRepository,
    private val clock: Clock,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _filter = MutableStateFlow<Category?>(null)

    val state: StateFlow<AnalyticsUiState> = benchmarkRepository.observeAllBenchmarks()
        .flatMapLatest { benchmarks ->
            if (benchmarks.isEmpty()) {
                flowOf(AnalyticsUiState())
            } else {
                val logFlows = benchmarks.map { benchmarkRepository.observeLogsFor(it.id) }
                combine(logFlows) { perBenchmarkLogs ->
                    val cards = benchmarks.mapIndexed { i, bm ->
                        val logs = perBenchmarkLogs[i]
                        BenchmarkAnalyticsCardState(
                            benchmark = bm,
                            series = BenchmarkProgressBuilder.build(bm, logs),
                            latestRawValue = logs.maxByOrNull { it.loggedAt }?.rawValue,
                            delta = benchmarkDelta(
                                logs = logs,
                                nowEpochMillis = clock.now(),
                                better = benchmarkBetter(bm.id),
                                zoneId = zoneId,
                            ),
                        )
                    }
                    AnalyticsUiState(
                        cards = cards,
                        allCategories = Category.values().filter { c -> benchmarks.any { it.category == c } },
                    )
                }
            }
        }
        .combine(_filter) { state, filter -> state.copy(filter = filter) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AnalyticsUiState(),
        )

    fun onFilterChanged(filter: Category?) {
        _filter.value = filter
    }
}
