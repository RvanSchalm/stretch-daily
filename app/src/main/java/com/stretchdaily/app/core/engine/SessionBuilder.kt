package com.stretchdaily.app.core.engine

import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.model.FlexibilityTier
import javax.inject.Inject
import kotlin.random.Random

/**
 * Picks a session worth of exercises out of the catalog given a category-weight
 * map and a set of forced (Selection Shield) exercise ids.
 *
 * Three phases per `build` call:
 *
 *  Phase A — **forced**: insert up to [Config.maxForced] exercises from
 *           [forcedExerciseIds], oldest-stale first. This guarantees rotation
 *           without letting a fresh DB (where everything is "stale") dump all
 *           46 exercises into one session.
 *
 *  Phase B — **weighted fill**: keep drawing exercises by weighted random,
 *           where each exercise's selection weight equals its category's
 *           weight, until we have at least [Config.minExercises] AND at least
 *           [Config.minSeconds]. Stops when [Config.maxExercises] is reached
 *           or no candidates remain.
 *
 *  Each picked exercise's contribution is `min(totalTime, perExerciseCapSeconds)`.
 *  This caps any monster exercise (e.g. K01 Backward Walk at 120 s) at 120 s of
 *  the session budget — defensive even though no current exercise blows past
 *  that.
 */
class SessionBuilder @Inject constructor() {

    fun build(
        exercises: List<Exercise>,
        weights: Map<Category, Double>,
        forcedExerciseIds: Set<String>,
        config: Config = Config(),
        random: Random = Random.Default,
    ): SessionPlan {
        val plan = mutableListOf<PlannedExercise>()
        val used = mutableSetOf<String>()
        var totalSeconds = 0

        // Phase A — forced (Selection Shield).
        val forcedPool = exercises
            .filter { it.id in forcedExerciseIds }
            // Nulls (never performed) are stalest, then oldest lastPerformed first.
            .sortedWith(compareBy(nullsFirst()) { it.lastPerformed })
            .take(config.maxForced)

        for (ex in forcedPool) {
            if (plan.size >= config.maxExercises) break
            val seconds = ex.totalTime.coerceAtMost(config.perExerciseCapSeconds)
            plan += PlannedExercise(ex, seconds, isForced = true)
            used += ex.id
            totalSeconds += seconds
        }

        // Phase B — weighted random fill until we hit both minimums.
        val remaining = exercises.filter { it.id !in used }.toMutableList()
        while (remaining.isNotEmpty() && plan.size < config.maxExercises) {
            val belowMinExercises = plan.size < config.minExercises
            val belowMinSeconds = totalSeconds < config.minSeconds
            if (!belowMinExercises && !belowMinSeconds) break

            val pick = weightedPick(remaining, weights, random)
            remaining.remove(pick)
            val seconds = pick.totalTime.coerceAtMost(config.perExerciseCapSeconds)
            plan += PlannedExercise(pick, seconds, isForced = false)
            used += pick.id
            totalSeconds += seconds
        }

        return SessionPlan(
            items = plan,
            totalSeconds = totalSeconds,
            categoryWeights = weights,
        )
    }

    private fun weightedPick(
        candidates: List<Exercise>,
        weights: Map<Category, Double>,
        random: Random,
    ): Exercise {
        val totalWeight = candidates.sumOf {
            (weights[it.category] ?: FlexibilityTier.AVERAGE.weight).coerceAtLeast(0.0)
        }
        if (totalWeight <= 0.0) return candidates.random(random)

        var r = random.nextDouble() * totalWeight
        for (c in candidates) {
            val w = (weights[c.category] ?: FlexibilityTier.AVERAGE.weight).coerceAtLeast(0.0)
            r -= w
            if (r <= 0.0) return c
        }
        return candidates.last()
    }

    data class Config(
        val minSeconds: Int = 600,
        val maxSeconds: Int = 900,
        val minExercises: Int = 5,
        val maxExercises: Int = 8,
        val perExerciseCapSeconds: Int = 120,
        val maxForced: Int = 2,
    )
}
