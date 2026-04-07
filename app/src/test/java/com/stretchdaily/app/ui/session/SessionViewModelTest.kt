package com.stretchdaily.app.ui.session

import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.engine.LongevityEngine
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var engine: LongevityEngine
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: SessionRepository
    private lateinit var clock: Clock

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        engine = mockk()
        exerciseDao = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        clock = Clock { 1_700_000_000_000L }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun exercise(
        id: String,
        category: Category = Category.NECK,
        isUnilateral: Boolean = false,
        totalTime: Int = 60,
    ) = Exercise(
        id = id,
        name = id,
        category = category,
        cues = listOf("cue 1", "cue 2"),
        isUnilateral = isUnilateral,
        isTimed = true,
        targetReps = null,
        secondsPerRep = null,
        totalTime = totalTime,
    )

    private fun planned(
        id: String,
        seconds: Int = 60,
        isUnilateral: Boolean = false,
    ) = PlannedExercise(
        exercise = exercise(id, isUnilateral = isUnilateral, totalTime = seconds),
        effectiveSeconds = seconds,
        isForced = false,
    )

    private fun plan(vararg items: PlannedExercise): SessionPlan = SessionPlan(
        items = items.toList(),
        totalSeconds = items.sumOf { it.effectiveSeconds },
        categoryWeights = emptyMap(),
    )

    @Test
    fun `init transitions to Preview after generate succeeds`() = runTest {
        coEvery { engine.generateSession() } returns plan(planned("a"), planned("b"))

        val vm = SessionViewModel(engine, exerciseDao, repository, clock)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("expected Preview, was $state", state is SessionUiState.Preview)
        assertEquals(2, (state as SessionUiState.Preview).plan.items.size)
    }

    @Test
    fun `init transitions to Error when engine throws`() = runTest {
        coEvery { engine.generateSession() } throws IllegalStateException("boom")

        val vm = SessionViewModel(engine, exerciseDao, repository, clock)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is SessionUiState.Error)
        assertEquals("boom", (state as SessionUiState.Error).message)
    }

    @Test
    fun `start transitions Preview to FollowAlong with first item`() = runTest {
        coEvery { engine.generateSession() } returns plan(planned("a", seconds = 30), planned("b", seconds = 30))

        val vm = SessionViewModel(engine, exerciseDao, repository, clock)
        advanceUntilIdle()
        vm.start()

        val state = vm.state.value
        assertTrue(state is SessionUiState.FollowAlong)
        val follow = state as SessionUiState.FollowAlong
        assertEquals(0, follow.currentIndex)
        assertEquals(Side.NONE, follow.side)
        assertEquals(30, follow.remainingSeconds)
        assertEquals(30, follow.totalSecondsForPhase)
    }

    @Test
    fun `start on unilateral exercise begins on LEFT side`() = runTest {
        coEvery { engine.generateSession() } returns plan(
            planned("a", seconds = 60, isUnilateral = true)
        )

        val vm = SessionViewModel(engine, exerciseDao, repository, clock)
        advanceUntilIdle()
        vm.start()

        val follow = vm.state.value as SessionUiState.FollowAlong
        assertEquals(Side.LEFT, follow.side)
        // 60s split with LEFT getting the ceiling -> 30 each
        assertEquals(30, follow.remainingSeconds)
    }

    @Test
    fun `tick decrements remainingSeconds`() = runTest {
        coEvery { engine.generateSession() } returns plan(planned("a", seconds = 5))

        val vm = SessionViewModel(engine, exerciseDao, repository, clock)
        advanceUntilIdle()
        vm.start()

        vm.tick()
        assertEquals(4, (vm.state.value as SessionUiState.FollowAlong).remainingSeconds)
        vm.tick()
        assertEquals(3, (vm.state.value as SessionUiState.FollowAlong).remainingSeconds)
    }

    @Test
    fun `paused tick is a no-op`() = runTest {
        coEvery { engine.generateSession() } returns plan(planned("a", seconds = 5))

        val vm = SessionViewModel(engine, exerciseDao, repository, clock)
        advanceUntilIdle()
        vm.start()
        vm.togglePause()

        vm.tick()
        vm.tick()
        vm.tick()

        val follow = vm.state.value as SessionUiState.FollowAlong
        assertEquals(5, follow.remainingSeconds)
        assertTrue(follow.isPaused)
    }

    @Test
    fun `tick rolling over to zero advances to next exercise`() = runTest {
        coEvery { engine.generateSession() } returns plan(
            planned("a", seconds = 1),
            planned("b", seconds = 30),
        )

        val vm = SessionViewModel(engine, exerciseDao, repository, clock)
        advanceUntilIdle()
        vm.start()

        vm.tick() // a: 1 -> 0 -> advance to b

        val follow = vm.state.value as SessionUiState.FollowAlong
        assertEquals(1, follow.currentIndex)
        assertEquals(30, follow.remainingSeconds)
    }

    @Test
    fun `unilateral LEFT advances to RIGHT before next exercise`() = runTest {
        coEvery { engine.generateSession() } returns plan(
            planned("a", seconds = 2, isUnilateral = true),
            planned("b", seconds = 10),
        )

        val vm = SessionViewModel(engine, exerciseDao, repository, clock)
        advanceUntilIdle()
        vm.start()

        // 2s budget with LEFT getting ceiling -> LEFT 1s, RIGHT 1s
        vm.tick() // LEFT 1 -> 0 -> RIGHT
        var follow = vm.state.value as SessionUiState.FollowAlong
        assertEquals(0, follow.currentIndex)
        assertEquals(Side.RIGHT, follow.side)
        assertEquals(1, follow.remainingSeconds)

        vm.tick() // RIGHT 1 -> 0 -> next exercise b
        follow = vm.state.value as SessionUiState.FollowAlong
        assertEquals(1, follow.currentIndex)
        assertEquals(Side.NONE, follow.side)
    }

    @Test
    fun `finishing the last exercise persists and transitions to Complete`() = runTest {
        val theOnly = planned("only", seconds = 1)
        coEvery { engine.generateSession() } returns plan(theOnly)
        coEvery { repository.completeSession(any(), any()) } returns 1L
        coEvery { repository.currentStreakDays() } returns 7

        val vm = SessionViewModel(engine, exerciseDao, repository, clock)
        advanceUntilIdle()
        vm.start()
        vm.tick() // 1 -> 0 -> finish
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("expected Complete, was $state", state is SessionUiState.Complete)
        val complete = state as SessionUiState.Complete
        assertEquals(1, complete.exerciseCount)
        assertEquals(1, complete.totalSeconds)
        assertEquals(7, complete.streakDays)
        coVerify { repository.completeSession(any(), any()) }
    }

    @Test
    fun `skip jumps straight to next exercise`() = runTest {
        coEvery { engine.generateSession() } returns plan(
            planned("a", seconds = 60),
            planned("b", seconds = 60),
        )

        val vm = SessionViewModel(engine, exerciseDao, repository, clock)
        advanceUntilIdle()
        vm.start()
        vm.skip()

        val follow = vm.state.value as SessionUiState.FollowAlong
        assertEquals(1, follow.currentIndex)
    }

    @Test
    fun `phaseSeconds bilateral returns full budget`() {
        val item = planned("a", seconds = 60, isUnilateral = false)
        assertEquals(60, SessionViewModel.phaseSeconds(item, Side.NONE))
    }

    @Test
    fun `phaseSeconds unilateral splits with ceiling on LEFT`() {
        val item = planned("a", seconds = 61, isUnilateral = true)
        assertEquals(31, SessionViewModel.phaseSeconds(item, Side.LEFT))
        assertEquals(30, SessionViewModel.phaseSeconds(item, Side.RIGHT))
    }
}
