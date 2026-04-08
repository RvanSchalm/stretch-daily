package com.stretchdaily.app.ui.benchmarks

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier

/**
 * Top-level state for [BenchmarksScreen]. Holds the full benchmark catalog
 * plus the most recent log for each (if any).
 */
sealed interface BenchmarksUiState {
    data object Loading : BenchmarksUiState
    data class Loaded(
        val rows: List<BenchmarkRow>,
    ) : BenchmarksUiState
    data class Error(val message: String) : BenchmarksUiState
}

/** Catalog entry + its latest log, if the user has ever logged it. */
data class BenchmarkRow(
    val benchmark: Benchmark,
    val latestLog: BenchmarkLog?,
)

/**
 * State of the "log a benchmark" dialog — used both for fresh entries
 * ([editingLogId] null) and in-place edits ([editingLogId] set to the row id).
 * [selectedTier] is only meaningful for the categorical benchmark; numeric
 * benchmarks use [rawInput].
 */
data class LogDialogState(
    val benchmark: Benchmark,
    val editingLogId: Long?,
    val rawInput: String,
    val selectedTier: FlexibilityTier?,
    val error: String?,
)
