package com.stretchdaily.app.core.session

import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.engine.LongevityEngine
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.util.Clock
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM coverage for the [TodaySessionHolder] state machine per spec §6.6.
 * Six scenarios: first ensureFresh, same-day cache, day rollover, swap,
 * completion no-op, swap after completion.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodaySessionHolderTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun epoch(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant().toEpochMilli()

    private fun exercise(id: String, category: Category = Category.HIPS): Exercise =
        Exercise(
            id = id,
            name = "Exercise $id",
            category = category,
            cues = emptyList(),
            isUnilateral = false,
            isTimed = true,
            targetReps = null,
            secondsPerRep = null,
            totalTime = 60,
            lastPerformed = null,
        )

    private fun plan(vararg ids: String): SessionPlan =
        SessionPlan(
            items = ids.map { id ->
                PlannedExercise(exercise(id), effectiveSeconds = 60, isForced = false)
            },
            totalSeconds = ids.size * 60,
            categoryWeights = emptyMap(),
        )

    /**
     * Builds a holder with deterministic collaborators. Each test owns a
     * closure for the clock so it can advance days as needed.
     */
    private fun holder(
        engine: LongevityEngine,
        exerciseDao: ExerciseDao = mockk(relaxed = true),
        nowProvider: () -> Long,
    ): TodaySessionHolder {
        val scope: CoroutineScope = TestScope(UnconfinedTestDispatcher())
        val clock = Clock { nowProvider() }
        return TodaySessionHolder(
            engine = engine,
            exerciseDao = exerciseDao,
            clock = clock,
            scope = scope,
        )
    }

    @Test
    fun `first ensureFresh generates via engine and emits a TodaySession`() = runTest {
        val engine = mockk<LongevityEngine>()
        val expected = plan("E1", "E2")
        coEvery { engine.generateSession() } returns expected

        val h = holder(engine) { epoch(2026, 4, 23) }
        assertNull(h.state.value)
        h.ensureFresh(zoneId = zone)

        val today = h.state.value
        assertNotNull(today)
        assertEquals(LocalDate.of(2026, 4, 23), today!!.planDate)
        assertSame(expected, today.plan)
    }

    @Test
    fun `second ensureFresh same day returns cached plan without re-invoking engine`() = runTest {
        val engine = mockk<LongevityEngine>()
        var callCount = 0
        coEvery { engine.generateSession() } answers {
            callCount++
            plan("E$callCount")
        }

        val h = holder(engine) { epoch(2026, 4, 23) }
        h.ensureFresh(zoneId = zone)
        h.ensureFresh(zoneId = zone)

        assertEquals(1, callCount)
        assertEquals("E1", h.state.value?.plan?.items?.first()?.exercise?.id)
    }

    @Test
    fun `ensureFresh regenerates on calendar-day rollover`() = runTest {
        val engine = mockk<LongevityEngine>()
        var callCount = 0
        coEvery { engine.generateSession() } answers {
            callCount++
            plan("DAY$callCount")
        }

        var nowMillis = epoch(2026, 4, 23)
        val h = holder(engine) { nowMillis }
        h.ensureFresh(zoneId = zone)
        assertEquals(LocalDate.of(2026, 4, 23), h.state.value?.planDate)

        // Advance the clock to the next calendar day.
        nowMillis = epoch(2026, 4, 24)
        h.ensureFresh(zoneId = zone)
        assertEquals(LocalDate.of(2026, 4, 24), h.state.value?.planDate)
        assertEquals("DAY2", h.state.value?.plan?.items?.first()?.exercise?.id)
        assertEquals(2, callCount)
    }

    @Test
    fun `swap replaces the targeted exercise and preserves order and others`() = runTest {
        val engine = mockk<LongevityEngine>()
        coEvery { engine.generateSession() } returns plan("A", "B", "C")

        val exerciseDao = mockk<ExerciseDao>()
        coEvery { exerciseDao.getById("Z") } returns exercise("Z")

        val h = holder(engine, exerciseDao) { epoch(2026, 4, 23) }
        h.ensureFresh(zoneId = zone)
        h.swap(oldId = "B", newId = "Z")

        val ids = h.state.value!!.plan.items.map { it.exercise.id }
        assertEquals(listOf("A", "Z", "C"), ids)
    }

    @Test
    fun `onSessionCompleted is a no-op and same-day access returns the same plan`() = runTest {
        val engine = mockk<LongevityEngine>()
        var callCount = 0
        coEvery { engine.generateSession() } answers {
            callCount++
            plan("E$callCount")
        }

        val h = holder(engine) { epoch(2026, 4, 23) }
        h.ensureFresh(zoneId = zone)
        val before = h.state.value

        h.onSessionCompleted()
        h.ensureFresh(zoneId = zone)
        val after = h.state.value

        assertSame(before, after)
        assertEquals(1, callCount)
    }

    @Test
    fun `swap works after completion`() = runTest {
        val engine = mockk<LongevityEngine>()
        coEvery { engine.generateSession() } returns plan("A", "B")
        val exerciseDao = mockk<ExerciseDao>()
        coEvery { exerciseDao.getById("Z") } returns exercise("Z")

        val h = holder(engine, exerciseDao) { epoch(2026, 4, 23) }
        h.ensureFresh(zoneId = zone)
        h.onSessionCompleted()
        h.swap(oldId = "A", newId = "Z")

        val ids = h.state.value!!.plan.items.map { it.exercise.id }
        assertEquals(listOf("Z", "B"), ids)
        assertTrue(h.state.value!!.plan.items.first().exercise.id == "Z")
    }
}
