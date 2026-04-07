package com.stretchdaily.app.core.engine

import com.stretchdaily.app.core.database.dao.BenchmarkDao
import com.stretchdaily.app.core.database.dao.BenchmarkLogDao
import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.util.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public entry point for session generation.
 *
 * Loads the catalog + latest benchmark logs, runs them through the three pure
 * components ([CategoryWeightCalculator], [SelectionShield], [SessionBuilder]),
 * and returns a [SessionPlan] for the UI to render.
 *
 * This class is the only engine type that touches Room. All algorithmic
 * decisions live in the pure components so they're trivially unit-testable.
 */
@Singleton
class LongevityEngine @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val benchmarkDao: BenchmarkDao,
    private val benchmarkLogDao: BenchmarkLogDao,
    private val calculator: CategoryWeightCalculator,
    private val shield: SelectionShield,
    private val builder: SessionBuilder,
    private val clock: Clock,
) {

    suspend fun generateSession(): SessionPlan {
        val exercises = exerciseDao.getAll()
        val benchmarks = benchmarkDao.getAll()
        val latestLogs = benchmarkLogDao.getLatestPerBenchmark()
        val now = clock.now()

        val weights = calculator.calculate(latestLogs, benchmarks)
        val staleIds = shield.staleExercises(exercises, now).map { it.id }.toSet()

        return builder.build(
            exercises = exercises,
            weights = weights,
            forcedExerciseIds = staleIds,
        )
    }
}
