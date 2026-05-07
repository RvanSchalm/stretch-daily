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
 * Session-graph-scoped ViewModel.
 *
 * Phase model (per spec §5.2):
 *  - READY  : timer parked at full duration, awaiting Start tap. No countdown.
 *  - RUNNING: timer counting down at 1 Hz. Pause flips to PAUSED.
 *  - PAUSED : countdown frozen mid-phase. Resume flips back to RUNNING.
 *
 * Sound model (per spec §5.4): exactly one [audioPlayer.playEnd] call per
 * timer phase reaching zero. start/pause/resume/next/prev are silent.
 * Init does not chime — the user lands in READY in silence.
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
                phase = TimerPhase.READY,
            )
            // No audio on init — user lands in READY in silence.
        }
    }

    fun start() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        if (r.phase != TimerPhase.READY) return
        _state.value = r.copy(phase = TimerPhase.RUNNING)
        startTimer()
    }

    fun pause() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        if (r.phase != TimerPhase.RUNNING) return
        _state.value = r.copy(phase = TimerPhase.PAUSED)
    }

    fun resume() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        if (r.phase != TimerPhase.PAUSED) return
        _state.value = r.copy(phase = TimerPhase.RUNNING)
        startTimer()  // Defensive: re-launch the 1 Hz loop so we don't depend
                      // on the prior timerJob still being alive across pause/resume.
                      // startTimer() begins with timerJob?.cancel(), so this is
                      // idempotent if the prior job is still running.
    }

    fun next() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        timerJob?.cancel()
        advanceSilent(r)
    }

    fun prev() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        timerJob?.cancel()
        val item = r.plan.items[r.currentIndex]
        if (item.exercise.isUnilateral && r.side == Side.RIGHT) {
            // RIGHT → LEFT of same exercise, READY.
            val secs = phaseSeconds(item, Side.LEFT)
            _state.value = r.copy(
                side = Side.LEFT,
                remainingSeconds = secs,
                totalSecondsForPhase = secs,
                phase = TimerPhase.READY,
            )
            return
        }
        val prevIndex = (r.currentIndex - 1).coerceAtLeast(0)
        val prevItem = r.plan.items[prevIndex]
        // For unilateral, prev() lands on RIGHT (terminal side); for bilateral, NONE.
        val prevSide = if (prevItem.exercise.isUnilateral) Side.RIGHT else Side.NONE
        val secs = phaseSeconds(prevItem, prevSide)
        _state.value = r.copy(
            currentIndex = prevIndex,
            side = prevSide,
            remainingSeconds = secs,
            totalSecondsForPhase = secs,
            phase = TimerPhase.READY,
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

    /** Exposed to tests. Decrements when RUNNING; advances + chimes at zero. */
    internal fun tick() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        if (r.phase != TimerPhase.RUNNING) return
        val next = r.remainingSeconds - 1
        if (next > 0) {
            _state.value = r.copy(remainingSeconds = next)
            return
        }
        // Phase ended naturally: chime, then advance.
        timerJob?.cancel()
        viewModelScope.launch { audioPlayer.playEnd() }
        advanceSilent(r)
    }

    private fun advanceSilent(r: SessionPlayerUiState.Running) {
        val item = r.plan.items[r.currentIndex]
        // Unilateral LEFT → RIGHT, same exercise, READY.
        if (item.exercise.isUnilateral && r.side == Side.LEFT) {
            val secs = phaseSeconds(item, Side.RIGHT)
            _state.value = r.copy(
                side = Side.RIGHT,
                remainingSeconds = secs,
                totalSecondsForPhase = secs,
                phase = TimerPhase.READY,
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
            phase = TimerPhase.READY,
        )
    }

    private fun finish(plan: SessionPlan) {
        timerJob?.cancel()
        // Note: we do NOT call audioPlayer.playEnd() here. tick() already
        // fired one chime when the final phase reached zero. finish() is
        // also reachable via next() from the last phase, in which case the
        // user's session-complete cue is the visual transition (silent
        // completion is consistent with silent next() everywhere else).
        _state.value = SessionPlayerUiState.Complete(
            areasStretched = plan.items.map { it.exercise.category }.toSet().size,
            totalMinutes = plan.totalSeconds / 60,
            streakAfter = 0,
        )
        viewModelScope.launch {
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
        internal fun phaseSeconds(item: PlannedExercise, side: Side): Int =
            if (!item.exercise.isUnilateral || side == Side.NONE) {
                item.effectiveSeconds
            } else {
                (item.effectiveSeconds + if (side == Side.LEFT) 1 else 0) / 2
            }
    }
}
