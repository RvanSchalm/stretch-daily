package com.stretchdaily.app.ui.screen.carousel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.data.BenchmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BenchmarkCarouselViewModel @Inject constructor(
    private val repository: BenchmarkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BenchmarkCarouselUiState())
    val state: StateFlow<BenchmarkCarouselUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CarouselEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CarouselEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val benchmarks = repository.observeAllBenchmarks().first()
            _state.update { it.copy(isLoading = false, allBenchmarks = benchmarks) }
        }
    }

    fun setStep(i: Int) {
        _state.update { current ->
            val clamped = i.coerceIn(0, (current.totalSteps - 1).coerceAtLeast(0))
            current.copy(currentIndex = clamped, errorMessage = null)
        }
    }

    fun onSaveEntry(rawValue: String, tier: FlexibilityTier?) {
        val current = _state.value
        val benchmark = current.currentBenchmark ?: return
        viewModelScope.launch {
            val outcome: Result<Unit> = if (tier == null) {
                repository.logNumeric(benchmark.id, rawValue).map { }
            } else {
                runCatching { repository.logCategorical(benchmark.id, tier) }.map { }
            }
            outcome.fold(
                onSuccess = {
                    if (current.isLastStep) {
                        _events.emit(CarouselEvent.Finished)
                    } else {
                        _events.emit(CarouselEvent.Advance(current.currentIndex + 1))
                    }
                },
                onFailure = { t ->
                    _state.update { it.copy(errorMessage = t.message ?: "Could not save entry") }
                },
            )
        }
    }

    fun onClose() {
        viewModelScope.launch { _events.emit(CarouselEvent.Finished) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
