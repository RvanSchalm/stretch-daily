package com.stretchdaily.app.data

import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.database.dao.SessionDao
import com.stretchdaily.app.core.model.SessionRecord
import com.stretchdaily.app.core.util.Clock
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM coverage for the reactive flow properties on [SessionRepository].
 * The three flows wrap [SessionDao.observeAllRecords] with pure-math
 * transforms; we stub the DAO with [flowOf] so the math runs synchronously.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryFlowTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant().toEpochMilli()

    private fun record(completedAt: Long, durationSeconds: Int = 600): SessionRecord =
        SessionRecord(
            startedAt = completedAt - durationSeconds * 1000L,
            completedAt = completedAt,
            totalDurationSeconds = durationSeconds,
            exerciseCount = 7,
        )

    private fun repo(records: List<SessionRecord>, now: Long): SessionRepository {
        val sessionDao = mockk<SessionDao>(relaxed = true)
        coEvery { sessionDao.observeAllRecords() } returns flowOf(records)
        val exerciseDao = mockk<ExerciseDao>(relaxed = true)
        val clock = Clock { now }
        return SessionRepository(exerciseDao, sessionDao, clock)
    }

    @Test
    fun `streakFlow emits derived streak from observed records`() = runTest {
        val r = repo(
            listOf(record(midday(2026, 4, 22)), record(midday(2026, 4, 23))),
            now = midday(2026, 4, 23),
        )
        val streak = r.streakFlow(zoneId = zone).first()
        assertEquals(2, streak)
    }

    @Test
    fun `weeklyFlow scopes to Monday-through-today`() = runTest {
        // Thursday 2026-04-23. Week runs Mon 4/20 through Thu 4/23. A Sunday
        // session on 4/19 must be excluded.
        val r = repo(
            listOf(
                record(midday(2026, 4, 19)),
                record(midday(2026, 4, 21)),
                record(midday(2026, 4, 23)),
            ),
            now = midday(2026, 4, 23),
        )
        val week = r.weeklyFlow(zoneId = zone).first()
        assertEquals(
            setOf(LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 23)),
            week,
        )
    }

    @Test
    fun `totalsFlow sums durations and counts sessions`() = runTest {
        val r = repo(
            listOf(record(midday(2026, 4, 22), 600), record(midday(2026, 4, 23), 900)),
            now = midday(2026, 4, 23),
        )
        val totals = r.totalsFlow.first()
        assertEquals(2, totals.sessions)
        // 600 + 900 = 1500s = 25 min.
        assertEquals(25, totals.totalMinutes)
    }
}
