package com.stretchdaily.app.ui.screen.log

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier

data class BenchmarkLogUiState(
    val isLoading: Boolean = true,
    val groups: List<CategoryGroup> = emptyList(),
    val expandedRowId: String? = null,
    val sheet: LogSheetState = LogSheetState.Hidden,
    val errorMessage: String? = null,
)

data class CategoryGroup(
    val category: Category,
    val rows: List<BenchmarkRowUiState>,
)

data class BenchmarkRowUiState(
    val benchmark: Benchmark,
    val latestLog: BenchmarkLog?,
    val sparkline: List<Double>,
    val history: List<BenchmarkLog>, // newest-first, for the expanded table
    val isOverdue: Boolean,          // no log this calendar month
) {
    val latestTier: FlexibilityTier? get() = latestLog?.resolvedTier
}

sealed interface LogSheetState {
    data object Hidden : LogSheetState
    data class Visible(val benchmark: Benchmark) : LogSheetState
}
