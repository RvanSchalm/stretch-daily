package com.stretchdaily.app.ui.screen.dashboard

import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.Category
import java.time.LocalDate

/**
 * Rendered state for the Dashboard ("today") tab.
 *
 * `isLoading` guards the very first frame before [TodaySessionHolder] has
 * returned its plan; after that everything below is fully populated and
 * the screen renders the composed view.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.MIN,
    val streakDays: Int = 0,
    val weekCompleted: Set<LocalDate> = emptySet(),
    val plannedExercises: List<PlannedExercise> = emptyList(),
    val plannedMinutes: Int = 0,
    val extraFocus: Category? = null,
    val banner: BannerState = BannerState.Hidden,
    val kpis: Kpis = Kpis(),
)

/**
 * Whether and what the benchmark banner advertises. [Hidden] when the
 * preference is off or no benchmark is overdue.
 */
sealed interface BannerState {
    data object Hidden : BannerState
    data class Visible(
        val overdueCount: Int,
        val nextBenchmark: Benchmark?,
    ) : BannerState
}

/** 2×2 KPI grid: streak / total minutes / sessions / next benchmark. */
data class Kpis(
    val streakDays: Int = 0,
    val totalMinutes: Int = 0,
    val totalSessions: Int = 0,
    val nextBenchmarkDays: Int? = null,
)
