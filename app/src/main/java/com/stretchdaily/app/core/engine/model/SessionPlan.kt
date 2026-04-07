package com.stretchdaily.app.core.engine.model

import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise

/**
 * The output of [com.stretchdaily.app.core.engine.LongevityEngine.generateSession].
 *
 * The session UI consumes [items] in order, and uses [totalSeconds] for the
 * preview header. [categoryWeights] is exposed so the dashboard can show the
 * same weighting that drove the selection.
 */
data class SessionPlan(
    val items: List<PlannedExercise>,
    val totalSeconds: Int,
    val categoryWeights: Map<Category, Double>,
)

/**
 * One slot in a [SessionPlan].
 *
 * [effectiveSeconds] is what the engine budgets against — it equals
 * `min(exercise.totalTime, perExerciseCapSeconds)`. [isForced] is true when the
 * Selection Shield required this exercise; the UI may surface that with a
 * "rotation" badge so the user understands why it was picked.
 */
data class PlannedExercise(
    val exercise: Exercise,
    val effectiveSeconds: Int,
    val isForced: Boolean,
)
