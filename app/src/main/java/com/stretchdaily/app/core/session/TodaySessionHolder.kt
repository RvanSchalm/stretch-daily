package com.stretchdaily.app.core.session

import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.di.ApplicationScope
import com.stretchdaily.app.core.engine.LongevityEngine
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.util.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The in-memory cache for "today's session plan." Regenerates only on
 * calendar-day rollover; completion does not reset (per design spec §6,
 * Q5 tradeoff — user can redo the same session that day).
 *
 * - First access from any ViewModel calls [ensureFresh]; if the plan is
 *   absent or its planDate is stale, the engine regenerates.
 * - [swap] mutates one item in-place. No persistence — swaps are lost on
 *   process death, another accepted tradeoff.
 * - [onSessionCompleted] is a no-op on the cached plan. The actual
 *   persistence happens in the session player (R4), which talks to
 *   [com.stretchdaily.app.data.SessionRepository.completeSession] directly.
 *
 * [scope] is injected via [ApplicationScope] so if we ever need to launch
 * long-running work (e.g. prefetching tomorrow's plan at midnight) we have
 * a lifecycle-safe place to do it. Currently unused — held for future use.
 */
@Singleton
class TodaySessionHolder @Inject constructor(
    private val engine: LongevityEngine,
    private val exerciseDao: ExerciseDao,
    private val clock: Clock,
    @Suppress("unused") @ApplicationScope private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow<TodaySession?>(null)
    val state: StateFlow<TodaySession?> = _state.asStateFlow()

    /** Single-writer guard: prevents two ViewModels racing on the first access. */
    private val mutex = Mutex()

    /**
     * Regenerates today's plan if the cache is empty or stale. Called from
     * every consuming ViewModel's init block. Cheap no-op on subsequent
     * same-day calls.
     */
    suspend fun ensureFresh(zoneId: ZoneId = ZoneId.systemDefault()) {
        mutex.withLock {
            val today = LocalDate.ofInstant(Instant.ofEpochMilli(clock.now()), zoneId)
            val current = _state.value
            if (current == null || current.planDate != today) {
                val plan = engine.generateSession()
                _state.value = TodaySession(today, plan)
            }
        }
    }

    /**
     * Replace the item with id [oldId] with a freshly-loaded exercise keyed
     * by [newId]. Requires an active cached plan — callers must
     * [ensureFresh] first (the overview ViewModel does).
     *
     * Same-category enforcement happens in the calling ViewModel, which
     * picks candidates from [ExerciseDao.getByCategory].
     */
    suspend fun swap(oldId: String, newId: String) {
        val newExercise = exerciseDao.getById(newId)
            ?: error("swap: exercise $newId not found")
        mutex.withLock {
            val current = _state.value
                ?: error("swap called before ensureFresh emitted a plan")
            val items = current.plan.items.map { item ->
                if (item.exercise.id == oldId) {
                    PlannedExercise(
                        exercise = newExercise,
                        effectiveSeconds = newExercise.totalTime.coerceAtMost(
                            PER_EXERCISE_CAP_SECONDS,
                        ),
                        isForced = false,
                    )
                } else {
                    item
                }
            }
            val newPlan = current.plan.copy(
                items = items,
                totalSeconds = items.sumOf { it.effectiveSeconds },
            )
            _state.value = current.copy(plan = newPlan)
        }
    }

    /**
     * Called by the session player (R4) after the record is persisted. By
     * design this is a no-op on the cached plan — the user can redo
     * today's session. Day rollover picks up a fresh plan.
     */
    fun onSessionCompleted() {
        // Intentionally empty. See class-level KDoc.
    }

    companion object {
        /**
         * Matches SessionBuilder.DEFAULT_PER_EXERCISE_CAP_SECONDS. Kept as a
         * local constant so this class doesn't import engine internals.
         */
        private const val PER_EXERCISE_CAP_SECONDS = 120
    }
}

/**
 * The value held by [TodaySessionHolder.state]. Null before first
 * [TodaySessionHolder.ensureFresh] call; non-null afterward.
 */
data class TodaySession(
    val planDate: LocalDate,
    val plan: SessionPlan,
)
