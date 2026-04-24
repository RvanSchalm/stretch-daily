package com.stretchdaily.app.ui.screen.carousel

import com.stretchdaily.app.core.model.Benchmark

data class BenchmarkCarouselUiState(
    val isLoading: Boolean = true,
    val allBenchmarks: List<Benchmark> = emptyList(),
    val currentIndex: Int = 0,
    val errorMessage: String? = null,
) {
    val totalSteps: Int get() = allBenchmarks.size
    val currentBenchmark: Benchmark? get() = allBenchmarks.getOrNull(currentIndex)
    val isLastStep: Boolean get() = currentIndex >= totalSteps - 1
}

/** One-shot events the screen listens for (save-and-advance or carousel-complete). */
sealed interface CarouselEvent {
    data class Advance(val toIndex: Int) : CarouselEvent
    data object Finished : CarouselEvent
}
