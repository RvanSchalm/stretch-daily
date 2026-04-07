package com.stretchdaily.app.ui.session

import com.stretchdaily.app.core.engine.model.SessionPlan

/**
 * All states the session flow can be in, from "engine is loading the plan" to
 * "user has finished and we're showing the summary".
 */
sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class Preview(val plan: SessionPlan) : SessionUiState
    data class FollowAlong(
        val plan: SessionPlan,
        val currentIndex: Int,
        val side: Side,
        val remainingSeconds: Int,
        val totalSecondsForPhase: Int,
        val isPaused: Boolean,
    ) : SessionUiState {
        val currentItem get() = plan.items[currentIndex]
        val progressFraction: Float
            get() =
                if (totalSecondsForPhase == 0) 0f
                else 1f - remainingSeconds.toFloat() / totalSecondsForPhase.toFloat()
    }

    data class Complete(
        val exerciseCount: Int,
        val totalSeconds: Int,
        val streakDays: Int,
    ) : SessionUiState

    data class Error(val message: String) : SessionUiState
}

/**
 * Which half of a unilateral exercise the user is currently on. [NONE] is for
 * bilateral exercises (torso rotations, butterfly, etc.) where there's no sides
 * distinction.
 */
enum class Side {
    NONE,
    LEFT,
    RIGHT,
}
