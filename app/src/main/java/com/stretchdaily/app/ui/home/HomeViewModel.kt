package com.stretchdaily.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.engine.CategoryWeightCalculator
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.data.BenchmarkRepository
import com.stretchdaily.app.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One row in the home dashboard heatmap. Carries the resolved [tier] used as
 * the row label and the underlying [weight] used to size the bar (a fractional
 * average across multiple benchmarks per category, so the bar is more
 * informative than the snap-to-tier label alone).
 */
data class CategoryHeatmapRow(
    val category: Category,
    val tier: FlexibilityTier,
    val weight: Double,
) {
    /** Stiff-or-worse rows get the orange highlight on the dashboard. */
    val isStiff: Boolean
        get() = tier == FlexibilityTier.STIFF || tier == FlexibilityTier.BELOW_AVERAGE
}

/** State rendered by [HomeScreen]. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val benchmarksDue: Boolean = false,
    val streakDays: Int = 0,
    val totalSessions: Int = 0,
    val sessionsThisWeek: Int = 0,
    val lastSessionAt: Long? = null,
    val heatmap: List<CategoryHeatmapRow> = emptyList(),
)

/**
 * ViewModel for the home dashboard. Pulls all the KPI data — streak, volume,
 * weekly count, last session timestamp, benchmarks-due banner, and the
 * per-category heatmap — in one [refresh] pass and emits a single
 * [HomeUiState]. The heatmap weights come from the same [CategoryWeightCalculator]
 * the engine uses, so the dashboard tells the user exactly which areas the
 * next session is going to bias toward.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val benchmarkRepository: BenchmarkRepository,
    private val sessionRepository: SessionRepository,
    private val categoryWeightCalculator: CategoryWeightCalculator,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Called on re-entry to Home so the banner and KPIs reflect the latest state. */
    fun refresh() {
        viewModelScope.launch {
            val due = benchmarkRepository.isBenchmarksDue()
            val streak = sessionRepository.currentStreakDays()
            val total = sessionRepository.totalSessions()
            val weekly = sessionRepository.sessionsThisWeek()
            val lastAt = sessionRepository.lastCompletedAt()
            val benchmarks = benchmarkRepository.getAllBenchmarks()
            val latestLogs = benchmarkRepository.getLatestLogs()
            val weights = categoryWeightCalculator.calculate(latestLogs, benchmarks)
            val rows = Category.entries.map { category ->
                val weight = weights[category] ?: FlexibilityTier.AVERAGE.weight
                CategoryHeatmapRow(
                    category = category,
                    tier = tierFromWeight(weight),
                    weight = weight,
                )
            }

            _state.value = HomeUiState(
                isLoading = false,
                benchmarksDue = due,
                streakDays = streak,
                totalSessions = total,
                sessionsThisWeek = weekly,
                lastSessionAt = lastAt,
                heatmap = rows,
            )
        }
    }

    companion object {
        /**
         * Snap a fractional category weight (averaged across multiple
         * benchmarks) to the closest [FlexibilityTier]. Used so the heatmap
         * label still says e.g. "Below Average" even when the underlying
         * weight is 2.25.
         */
        internal fun tierFromWeight(weight: Double): FlexibilityTier =
            FlexibilityTier.entries.minBy { abs(it.weight - weight) }
    }
}
