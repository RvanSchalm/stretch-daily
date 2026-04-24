package com.stretchdaily.app.ui.screen.session

import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan

sealed interface SessionPlayerUiState {
    data object Loading : SessionPlayerUiState

    /** Running / paused player state. */
    data class Running(
        val plan: SessionPlan,
        val currentIndex: Int,
        val side: Side,
        val remainingSeconds: Int,
        val totalSecondsForPhase: Int,
        val isPaused: Boolean,
    ) : SessionPlayerUiState {
        val currentItem: PlannedExercise get() = plan.items[currentIndex]
        val progressFraction: Float
            get() = if (totalSecondsForPhase == 0) 0f
                    else 1f - remainingSeconds.toFloat() / totalSecondsForPhase.toFloat()
    }

    /** Terminal — completion summary drives `SessionCompleteScreen`. */
    data class Complete(
        val areasStretched: Int,
        val totalMinutes: Int,
        val streakAfter: Int,
    ) : SessionPlayerUiState
}

/** Unilateral-side enum, preserved from the previous session VM. */
enum class Side { NONE, LEFT, RIGHT }
