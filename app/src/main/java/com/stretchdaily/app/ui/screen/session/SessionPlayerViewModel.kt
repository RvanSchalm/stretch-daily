package com.stretchdaily.app.ui.screen.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.audio.SessionAudioPlayer
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.session.TodaySessionHolder
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
 * Nested-graph-scoped ViewModel for `"session/player"` + `"session/complete"`.
 * Reads the plan from [TodaySessionHolder] on init, runs the 1 Hz timer
 * loop, writes the [SessionRepository.completeSession] record at the end,
 * and stays alive for the complete screen to read [SessionPlayerUiState.Complete].
 *
 * Timer: [startTimer] launches a coroutine that calls [tick] every second.
 * [tick] is `internal` so unit tests can drive it deterministically.
 */
@HiltViewModel
class SessionPlayerViewModel @Inject constructor(
    private val holder: TodaySessionHolder,
    private val repository: SessionRepository,
    private val audioPlayer: SessionAudioPlayer,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionPlayerUiState>(SessionPlayerUiState.Loading)
    val state: StateFlow<SessionPlayerUiState> = _state.asStateFlow()

    private var startedAt: Long = 0L
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            // Holder is already populated by the overview screen, but guard anyway.
            holder.ensureFresh()
            val today = holder.state.value ?: return@launch
            startedAt = clock.now()
            val first = today.plan.items.firstOrNull() ?: return@launch
            val side = if (first.exercise.isUnilateral) Side.LEFT else Side.NONE
            val secs = phaseSeconds(first, side)
            _state.value = SessionPlayerUiState.Running(
                plan = today.plan,
                currentIndex = 0,
                side = side,
                remainingSeconds = secs,
                totalSecondsForPhase = secs,
                isPaused = false,
            )
            audioPlayer.playStart()
            startTimer()
        }
    }

    fun togglePause() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        _state.value = r.copy(isPaused = !r.isPaused)
    }

    fun skip() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        advance(r)
    }

    fun prev() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        // Unilateral RIGHT → LEFT on the same exercise; otherwise step back one exercise.
        val item = r.plan.items[r.currentIndex]
        if (item.exercise.isUnilateral && r.side == Side.RIGHT) {
            val secs = phaseSeconds(item, Side.LEFT)
            _state.value = r.copy(side = Side.LEFT, remainingSeconds = secs, totalSecondsForPhase = secs)
            return
        }
        val prevIndex = (r.currentIndex - 1).coerceAtLeast(0)
        val prevItem = r.plan.items[prevIndex]
        val prevSide =
            if (prevItem.exercise.isUnilateral) Side.LEFT else Side.NONE
        val secs = phaseSeconds(prevItem, prevSide)
        _state.value = r.copy(
            currentIndex = prevIndex,
            side = prevSide,
            remainingSeconds = secs,
            totalSecondsForPhase = secs,
        )
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

    /** Exposed to tests. Decrements or advances when the phase reaches zero. */
    internal fun tick() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        if (r.isPaused) return
        val next = r.remainingSeconds - 1
        if (next > 0) {
            _state.value = r.copy(remainingSeconds = next)
        } else {
            advance(r)
        }
    }

    private fun advance(r: SessionPlayerUiState.Running) {
        val item = r.plan.items[r.currentIndex]

        // Unilateral LEFT → RIGHT same exercise.
        if (item.exercise.isUnilateral && r.side == Side.LEFT) {
            val secs = phaseSeconds(item, Side.RIGHT)
            _state.value = r.copy(
                side = Side.RIGHT,
                remainingSeconds = secs,
                totalSecondsForPhase = secs,
            )
            return
        }

        val nextIndex = r.currentIndex + 1
        if (nextIndex >= r.plan.items.size) {
            finish(r.plan)
            return
        }
        val nextItem = r.plan.items[nextIndex]
        val nextSide = if (nextItem.exercise.isUnilateral) Side.LEFT else Side.NONE
        val secs = phaseSeconds(nextItem, nextSide)
        _state.value = r.copy(
            currentIndex = nextIndex,
            side = nextSide,
            remainingSeconds = secs,
            totalSecondsForPhase = secs,
        )
        viewModelScope.launch { audioPlayer.playStart() }
    }

    private fun finish(plan: SessionPlan) {
        timerJob?.cancel()
        // Flip state synchronously so any in-flight tick() bails before we
        // kick off the persistence coroutine — prevents duplicate session
        // records if tick() re-enters finish() before the launch block below
        // has a chance to write _state.
        _state.value = SessionPlayerUiState.Complete(
            areasStretched = plan.items.map { it.exercise.category }.toSet().size,
            totalMinutes = plan.totalSeconds / 60,
            streakAfter = 0,
        )
        viewModelScope.launch {
            audioPlayer.playEnd()
            repository.completeSession(plan, startedAt)
            holder.onSessionCompleted()
            val streak = repository.currentStreakDays()
            val current = _state.value as? SessionPlayerUiState.Complete ?: return@launch
            _state.value = current.copy(streakAfter = streak)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        /**
         * Split an exercise's budget by side. Bilateral or `Side.NONE`: full
         * budget. Unilateral: half each, LEFT gets the ceiling so odd totals
         * don't lose a second. Preserved from the pre-R1 session VM.
         */
        internal fun phaseSeconds(item: PlannedExercise, side: Side): Int =
            if (!item.exercise.isUnilateral || side == Side.NONE) {
                item.effectiveSeconds
            } else {
                (item.effectiveSeconds + if (side == Side.LEFT) 1 else 0) / 2
            }
    }
}
