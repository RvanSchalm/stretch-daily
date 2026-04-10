package com.stretchdaily.app.ui.benchmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.data.BenchmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs both the main [BenchmarksScreen] (list of all 10 benchmarks + latest
 * value) and the individual log dialog. The dialog state is held here too so
 * the user can tap a row, enter a value, rotate the device, and still see
 * their pending input.
 *
 * The list state is derived reactively from the database so it stays in sync
 * with mutations made elsewhere — most importantly the Settings "Delete all
 * data" action, which would otherwise leave the cards displaying stale values
 * until the next manual refresh.
 */
@HiltViewModel
class BenchmarksViewModel @Inject constructor(
    private val repository: BenchmarkRepository,
) : ViewModel() {

    val state: StateFlow<BenchmarksUiState> = combine(
        repository.observeAllBenchmarks(),
        repository.observeLatestLogs(),
    ) { benchmarks, latestLogs ->
        val latestByBenchmark = latestLogs.associateBy { it.benchmarkId }
        val rows = benchmarks.map { b ->
            BenchmarkRow(benchmark = b, latestLog = latestByBenchmark[b.id])
        }
        BenchmarksUiState.Loaded(rows) as BenchmarksUiState
    }
        .catch { t ->
            emit(BenchmarksUiState.Error(t.message ?: "Failed to load benchmarks"))
        }
        .stateIn(
            scope = viewModelScope,
            // Collect eagerly so re-entering the Benchmarks tab from a
            // graph-scoped ViewModel always reflects the latest database
            // state — most importantly after Settings → "Delete all data"
            // wipes the logs while the user is on another tab.
            started = SharingStarted.Eagerly,
            initialValue = BenchmarksUiState.Loading,
        )

    private val _dialog = MutableStateFlow<LogDialogState?>(null)
    val dialog: StateFlow<LogDialogState?> = _dialog.asStateFlow()

    /** Open the log dialog to create a new entry for [benchmark]. */
    fun openLogDialog(benchmark: Benchmark) {
        _dialog.value = LogDialogState(
            benchmark = benchmark,
            editingLogId = null,
            rawInput = "",
            selectedTier = null,
            error = null,
        )
    }

    /** Open the log dialog pre-filled to edit an existing entry. */
    fun openEditDialog(benchmark: Benchmark, log: BenchmarkLog) {
        _dialog.value = LogDialogState(
            benchmark = benchmark,
            editingLogId = log.id,
            rawInput = log.rawValue,
            selectedTier = log.resolvedTier.takeIf {
                benchmark.inputType == BenchmarkInputType.CATEGORICAL
            },
            error = null,
        )
    }

    fun dismissDialog() {
        _dialog.value = null
    }

    fun onRawInputChange(newValue: String) {
        _dialog.update { it?.copy(rawInput = newValue, error = null) }
    }

    fun onTierSelect(tier: FlexibilityTier) {
        _dialog.update { it?.copy(selectedTier = tier, error = null) }
    }

    fun submitDialog(onSuccess: () -> Unit = {}) {
        val current = _dialog.value ?: return
        viewModelScope.launch {
            val result = when (current.benchmark.inputType) {
                BenchmarkInputType.NUMERIC -> submitNumeric(current)
                BenchmarkInputType.CATEGORICAL -> submitCategorical(current)
            }
            result.onSuccess {
                _dialog.value = null
                onSuccess()
            }.onFailure { t ->
                _dialog.update { it?.copy(error = t.message ?: "Failed to save") }
            }
        }
    }

    private suspend fun submitNumeric(current: LogDialogState): Result<Unit> {
        val raw = current.rawInput.trim()
        if (raw.isEmpty()) return Result.failure(IllegalArgumentException("Enter a value"))
        val existingId = current.editingLogId
        return if (existingId == null) {
            repository.logNumeric(current.benchmark.id, raw).map { }
        } else {
            val existing = repository.getLog(existingId)
                ?: return Result.failure(IllegalStateException("Log no longer exists"))
            repository.updateLog(
                existing = existing,
                rawValue = raw,
                benchmarkInputType = BenchmarkInputType.NUMERIC,
            )
        }
    }

    private suspend fun submitCategorical(current: LogDialogState): Result<Unit> {
        val tier = current.selectedTier
            ?: return Result.failure(IllegalArgumentException("Pick a tier"))
        val existingId = current.editingLogId
        return if (existingId == null) {
            runCatching { repository.logCategorical(current.benchmark.id, tier) }.map { }
        } else {
            val existing = repository.getLog(existingId)
                ?: return Result.failure(IllegalStateException("Log no longer exists"))
            repository.updateLog(
                existing = existing,
                rawValue = tier.name,
                tierOverride = tier,
                benchmarkInputType = BenchmarkInputType.CATEGORICAL,
            )
        }
    }

    fun deleteLog(log: BenchmarkLog) {
        viewModelScope.launch {
            repository.deleteLog(log.id)
        }
    }

    /** Flow for a single benchmark's log history — used by [BenchmarkHistoryScreen]. */
    fun observeLogsFor(benchmarkId: String) = repository.observeLogsFor(benchmarkId)

    /** One-shot lookup of a benchmark by id — used by [BenchmarkHistoryScreen] for the title. */
    suspend fun getBenchmark(id: String): Benchmark? = repository.getBenchmark(id)
}
