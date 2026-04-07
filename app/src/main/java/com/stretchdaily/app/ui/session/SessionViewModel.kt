package com.stretchdaily.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.engine.LongevityEngine
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Single source of truth for the session flow.
 *
 * Lifecycle:
 * 1. [init] calls [generate] which asks [LongevityEngine] for a [SessionPlan]
 *    and parks in [SessionUiState.Preview].
 * 2. [start] transitions to [SessionUiState.FollowAlong] at the first exercise
 *    and boots the 1 Hz countdown coroutine.
 * 3. Each tick decrements `remainingSeconds` until it hits 0, then [advance]
 *    moves to the next side (for unilateral exercises), next exercise, or
 *    calls [finish] if we've run out of items.
 * 4. [finish] persists via [SessionRepository] and transitions to
 *    [SessionUiState.Complete] with the new streak count.
 *
 * Pausing just sets a flag on the state — the timer coroutine stays alive but
 * no-ops on ticks while `isPaused == true`.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val engine: LongevityEngine,
    private val exerciseDao: ExerciseDao,
    private val repository: SessionRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private var startedAt: Long = 0L
    private var timerJob: Job? = null

    init {
        generate()
    }

    fun generate() {
        timerJob?.cancel()
        _state.value = SessionUiState.Loading
        viewModelScope.launch {
            try {
                val plan = engine.generateSession()
                _state.value = SessionUiState.Preview(plan)
            } catch (t: Throwable) {
                _state.value = SessionUiState.Error(t.message ?: "Failed to generate session")
            }
        }
    }

    fun swap(index: Int) {
        val preview = _state.value as? SessionUiState.Preview ?: return
        val current = preview.plan.items.getOrNull(index) ?: return
        viewModelScope.launch {
            val usedIds = preview.plan.items.map { it.exercise.id }.toSet()
            val candidates = exerciseDao
                .getByCategory(current.exercise.category)
                .filter { it.id !in usedIds }
            val replacement = candidates.randomOrNull() ?: return@launch
            val newItem = PlannedExercise(
                exercise = replacement,
                effectiveSeconds = replacement.totalTime.coerceAtMost(PER_EXERCISE_CAP),
                isForced = false,
            )
            val newItems = preview.plan.items.toMutableList().also { it[index] = newItem }
            val newTotal = newItems.sumOf { it.effectiveSeconds }
            _state.value = SessionUiState.Preview(
                preview.plan.copy(items = newItems, totalSeconds = newTotal)
            )
        }
    }

    fun start() {
        val preview = _state.value as? SessionUiState.Preview ?: return
        val firstItem = preview.plan.items.firstOrNull() ?: return
        startedAt = clock.now()
        val side = if (firstItem.exercise.isUnilateral) Side.LEFT else Side.NONE
        val phaseSeconds = phaseSeconds(firstItem, side)
        _state.value = SessionUiState.FollowAlong(
            plan = preview.plan,
            currentIndex = 0,
            side = side,
            remainingSeconds = phaseSeconds,
            totalSecondsForPhase = phaseSeconds,
            isPaused = false,
        )
        startTimer()
    }

    fun togglePause() {
        val current = _state.value as? SessionUiState.FollowAlong ?: return
        _state.value = current.copy(isPaused = !current.isPaused)
    }

    fun skip() {
        val current = _state.value as? SessionUiState.FollowAlong ?: return
        advance(current)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                tick()
            }
        }
    }

    /** Exposed as internal so unit tests can drive the timer deterministically. */
    internal fun tick() {
        val current = _state.value as? SessionUiState.FollowAlong ?: return
        if (current.isPaused) return
        val newRemaining = current.remainingSeconds - 1
        if (newRemaining > 0) {
            _state.value = current.copy(remainingSeconds = newRemaining)
        } else {
            advance(current)
        }
    }

    private fun advance(current: SessionUiState.FollowAlong) {
        val currentItem = current.plan.items[current.currentIndex]

        // Unilateral: after LEFT, move to RIGHT on the same exercise.
        if (currentItem.exercise.isUnilateral && current.side == Side.LEFT) {
            val secs = phaseSeconds(currentItem, Side.RIGHT)
            _state.value = current.copy(
                side = Side.RIGHT,
                remainingSeconds = secs,
                totalSecondsForPhase = secs,
            )
            return
        }

        // Otherwise move to next exercise, or finish if we're out.
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.plan.items.size) {
            finish(current.plan)
            return
        }
        val nextItem = current.plan.items[nextIndex]
        val nextSide = if (nextItem.exercise.isUnilateral) Side.LEFT else Side.NONE
        val nextSeconds = phaseSeconds(nextItem, nextSide)
        _state.value = current.copy(
            currentIndex = nextIndex,
            side = nextSide,
            remainingSeconds = nextSeconds,
            totalSecondsForPhase = nextSeconds,
        )
    }

    private fun finish(plan: SessionPlan) {
        timerJob?.cancel()
        viewModelScope.launch {
            repository.completeSession(plan, startedAt)
            val streak = repository.currentStreakDays()
            _state.value = SessionUiState.Complete(
                exerciseCount = plan.items.size,
                totalSeconds = plan.totalSeconds,
                streakDays = streak,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        private const val PER_EXERCISE_CAP = 120

        /**
         * Split an exercise's budget by side. For bilateral, full budget.
         * For unilateral, half each side — LEFT gets the ceiling so odd totals
         * don't lose a second.
         */
        internal fun phaseSeconds(item: PlannedExercise, side: Side): Int =
            if (!item.exercise.isUnilateral || side == Side.NONE) {
                item.effectiveSeconds
            } else {
                (item.effectiveSeconds + if (side == Side.LEFT) 1 else 0) / 2
            }
    }
}
