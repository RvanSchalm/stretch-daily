package com.stretchdaily.app.ui.screen.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BenchmarkLogViewModel @Inject constructor(
    private val repository: BenchmarkRepository,
    private val clock: Clock,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val _ephemeral = MutableStateFlow(EphemeralState())

    private val _state = MutableStateFlow(BenchmarkLogUiState())
    val state: StateFlow<BenchmarkLogUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val benchmarksFlow = repository.observeAllBenchmarks()
            val latestFlow = repository.observeLatestLogs()

            // For each catalog update, fan out into per-benchmark log flows.
            val derivedFlow: Flow<DerivedCatalog> =
                benchmarksFlow.flatMapLatest { benchmarks ->
                    if (benchmarks.isEmpty()) {
                        flowOf(DerivedCatalog(benchmarks = emptyList(), perBenchmarkLogs = emptyMap()))
                    } else {
                        val flows: List<Flow<Pair<String, List<BenchmarkLog>>>> =
                            benchmarks.map { bm ->
                                kotlinx.coroutines.flow.flow {
                                    repository.observeLogsFor(bm.id).collect { logs ->
                                        emit(bm.id to logs)
                                    }
                                }
                            }
                        combine(flows) { pairs ->
                            DerivedCatalog(
                                benchmarks = benchmarks,
                                perBenchmarkLogs = pairs.toMap(),
                            )
                        }
                    }
                }

            combine(derivedFlow, latestFlow, _ephemeral) { derived, latest, ephemeral ->
                buildState(derived, latest, ephemeral, clock.now(), zone)
            }.collect { next -> _state.value = next }
        }
    }

    fun onRowTapped(benchmarkId: String) {
        _ephemeral.update {
            it.copy(expandedRowId = if (it.expandedRowId == benchmarkId) null else benchmarkId)
        }
    }

    fun onLogPressed(benchmarkId: String) {
        val bm = _state.value.groups.asSequence()
            .flatMap { it.rows.asSequence() }
            .firstOrNull { it.benchmark.id == benchmarkId }?.benchmark
            ?: return
        _ephemeral.update { it.copy(sheet = LogSheetState.Visible(bm), errorMessage = null) }
    }

    fun onDismissSheet() {
        _ephemeral.update { it.copy(sheet = LogSheetState.Hidden, errorMessage = null) }
    }

    /**
     * Save entry. For numeric benchmarks pass [tierOverride] = null; for categorical pass the chosen tier.
     */
    fun onSubmit(rawValue: String, tierOverride: FlexibilityTier?) {
        val current = (_ephemeral.value.sheet as? LogSheetState.Visible)?.benchmark ?: return
        viewModelScope.launch {
            val outcome: Result<Unit> = if (tierOverride == null) {
                repository.logNumeric(current.id, rawValue).map { }
            } else {
                runCatching { repository.logCategorical(current.id, tierOverride) }.map { }
            }
            outcome.fold(
                onSuccess = {
                    _ephemeral.update { it.copy(sheet = LogSheetState.Hidden, errorMessage = null) }
                },
                onFailure = { t ->
                    _ephemeral.update { it.copy(errorMessage = t.message ?: "Could not save entry") }
                },
            )
        }
    }

    fun onDeleteLog(logId: Long) {
        viewModelScope.launch { repository.deleteLog(logId) }
    }

    fun dismissError() {
        _ephemeral.update { it.copy(errorMessage = null) }
    }

    // ────────────────────────────────────────────
    // Internal helpers

    private data class EphemeralState(
        val expandedRowId: String? = null,
        val sheet: LogSheetState = LogSheetState.Hidden,
        val errorMessage: String? = null,
    )

    private data class DerivedCatalog(
        val benchmarks: List<Benchmark>,
        val perBenchmarkLogs: Map<String, List<BenchmarkLog>>,
    )

    private fun buildState(
        derived: DerivedCatalog,
        latest: List<BenchmarkLog>,
        ephemeral: EphemeralState,
        nowMillis: Long,
        zone: ZoneId,
    ): BenchmarkLogUiState {
        val latestById = latest.associateBy { it.benchmarkId }
        val todayFirstOfMonth = LocalDate.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
            .withDayOfMonth(1)

        val rows = derived.benchmarks.map { bm ->
            val allLogs = derived.perBenchmarkLogs[bm.id].orEmpty()
            val latestLog = latestById[bm.id]
            val isOverdue = isOverdue(latestLog, todayFirstOfMonth, zone)
            BenchmarkRowUiState(
                benchmark = bm,
                latestLog = latestLog,
                sparkline = sparklineValues(allLogs, nowMillis),
                history = allLogs.sortedByDescending { it.loggedAt },
                isOverdue = isOverdue,
            )
        }

        // Group by category in enum order.
        val grouped = rows
            .groupBy { it.benchmark.category }
            .toSortedMap(compareBy { it.ordinal })
            .map { (category, rs) -> CategoryGroup(category, rs) }

        return BenchmarkLogUiState(
            isLoading = false,
            groups = grouped,
            expandedRowId = ephemeral.expandedRowId,
            sheet = ephemeral.sheet,
            errorMessage = ephemeral.errorMessage,
        )
    }

    private fun isOverdue(
        latestLog: BenchmarkLog?,
        monthStart: LocalDate,
        zone: ZoneId,
    ): Boolean {
        if (latestLog == null) return true
        val loggedDate = LocalDate.ofInstant(Instant.ofEpochMilli(latestLog.loggedAt), zone)
        return loggedDate.isBefore(monthStart)
    }

    companion object {
        /** Visible for tests — shared tier category-order comparator could live here if needed. */
        @Suppress("unused")
        private val CATEGORY_ORDER: Comparator<Category> = compareBy { it.ordinal }
    }
}
