package com.stretchdaily.app.data

import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.database.dao.SessionDao
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.SessionExercise
import com.stretchdaily.app.core.model.SessionRecord
import com.stretchdaily.app.core.util.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * One stop for reading and writing session history. The [SessionViewModel] talks
 * to this instead of poking DAOs directly so the view layer stays free of Room.
 *
 * Responsibilities:
 * - Persist a completed [SessionPlan] to `session_records` + `session_exercises`.
 * - Stamp each performed exercise's `lastPerformed` so the Selection Shield
 *   stops forcing it on the next run.
 * - Compute the user's current streak of consecutive calendar days with at
 *   least one completed session (with a 1-day grace window so opening the app
 *   late in the evening doesn't look like a miss).
 */
@Singleton
class SessionRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val sessionDao: SessionDao,
    private val clock: Clock,
) {

    suspend fun completeSession(plan: SessionPlan, startedAt: Long): Long {
        val completedAt = clock.now()
        val record = SessionRecord(
            startedAt = startedAt,
            completedAt = completedAt,
            totalDurationSeconds = plan.totalSeconds,
            exerciseCount = plan.items.size,
        )
        val sessionId = sessionDao.insertRecord(record)
        val rows = plan.items.mapIndexed { index, item ->
            SessionExercise(
                sessionId = sessionId,
                exerciseId = item.exercise.id,
                orderIndex = index,
                durationSeconds = item.effectiveSeconds,
            )
        }
        sessionDao.insertExercises(rows)
        for (item in plan.items) {
            exerciseDao.updateLastPerformed(item.exercise.id, completedAt)
        }
        return sessionId
    }

    suspend fun currentStreakDays(zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val timestamps = sessionDao.getAllCompletionTimestamps()
        return computeStreak(timestamps, clock.now(), zoneId)
    }

    /** Total number of completed sessions ever. */
    suspend fun totalSessions(): Int = sessionDao.getTotalCount()

    /** Epoch millis of the most recently completed session, or null if none. */
    suspend fun lastCompletedAt(): Long? = sessionDao.getLastCompletedAt()

    /**
     * Number of completed sessions in the last seven calendar days (inclusive
     * of today). Used by the dashboard "this week" KPI card.
     */
    suspend fun sessionsThisWeek(zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val timestamps = sessionDao.getAllCompletionTimestamps()
        return countSessionsInLastDays(timestamps, clock.now(), days = 7, zoneId = zoneId)
    }

    /** All session records, newest first. Backs the session history screen. */
    fun observeAllSessions(): Flow<List<SessionRecord>> = sessionDao.observeAllRecords()

    companion object {
        /**
         * Pure streak math, kept public so it can be unit-tested without Room.
         * Returns the number of consecutive calendar days (in [zoneId]) with at
         * least one completed session, anchored on today OR yesterday — the
         * yesterday fallback is the grace window.
         */
        internal fun computeStreak(
            completionTimestamps: List<Long>,
            now: Long,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): Int {
            if (completionTimestamps.isEmpty()) return 0
            val days = completionTimestamps
                .mapTo(hashSetOf()) { LocalDate.ofInstant(Instant.ofEpochMilli(it), zoneId) }
            val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zoneId)

            var anchor = when {
                today in days -> today
                today.minusDays(1) in days -> today.minusDays(1)
                else -> return 0
            }

            var streak = 0
            while (anchor in days) {
                streak++
                anchor = anchor.minusDays(1)
            }
            return streak
        }

        /**
         * Pure count of completion timestamps that fall within the trailing
         * [days]-day window ending today (inclusive). Extracted so it can be
         * unit tested without Room.
         */
        internal fun countSessionsInLastDays(
            completionTimestamps: List<Long>,
            now: Long,
            days: Int,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): Int {
            require(days >= 1) { "days must be >= 1" }
            val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zoneId)
            val earliest = today.minusDays((days - 1).toLong())
            return completionTimestamps.count {
                val date = LocalDate.ofInstant(Instant.ofEpochMilli(it), zoneId)
                !date.isBefore(earliest) && !date.isAfter(today)
            }
        }
    }
}
