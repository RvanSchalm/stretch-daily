package com.stretchdaily.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.data.BenchmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** State rendered by [HomeScreen]. */
data class HomeUiState(
    val benchmarksDue: Boolean = false,
)

/**
 * Minimal ViewModel for the Home screen — its only job right now is to
 * compute whether the benchmarks-due nag banner should show. Phase 5 will
 * grow this into the real dashboard (streak, volume, heatmap).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val benchmarkRepository: BenchmarkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Called on re-entry to Home so the banner reflects the latest state. */
    fun refresh() {
        viewModelScope.launch {
            _state.value = HomeUiState(benchmarksDue = benchmarkRepository.isBenchmarksDue())
        }
    }
}
