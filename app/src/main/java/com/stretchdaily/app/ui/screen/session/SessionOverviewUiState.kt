package com.stretchdaily.app.ui.screen.session

import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.model.Exercise

data class SessionOverviewUiState(
    val isLoading: Boolean = true,
    val planMinutes: Int = 0,
    val items: List<PlannedExercise> = emptyList(),
    val swap: SwapSheetState = SwapSheetState.Hidden,
)

sealed interface SwapSheetState {
    data object Hidden : SwapSheetState
    data class Visible(
        val oldItem: PlannedExercise,
        val candidates: List<Exercise>,
    ) : SwapSheetState
}
