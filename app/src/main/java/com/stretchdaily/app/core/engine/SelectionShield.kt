package com.stretchdaily.app.core.engine

import com.stretchdaily.app.core.model.Exercise
import javax.inject.Inject

/**
 * Finds exercises that have gone too long without being performed so the
 * [SessionBuilder] can force them back into rotation.
 *
 * An exercise is "stale" if [Exercise.lastPerformed] is null (never done) or
 * older than [staleAfterMillis] ago.
 */
class SelectionShield @Inject constructor() {

    fun staleExercises(
        exercises: List<Exercise>,
        now: Long,
        staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS,
    ): List<Exercise> {
        val cutoff = now - staleAfterMillis
        return exercises.filter { it.lastPerformed == null || it.lastPerformed < cutoff }
    }

    companion object {
        val DEFAULT_STALE_AFTER_MILLIS: Long = 14L * 24 * 60 * 60 * 1000
    }
}
