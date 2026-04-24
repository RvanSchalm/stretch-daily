package com.stretchdaily.app.ui.screen.session

import com.stretchdaily.app.core.audio.SessionAudioPlayer
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.session.TodaySession
import com.stretchdaily.app.core.session.TodaySessionHolder
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.SessionRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class SessionPlayerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var holder: TodaySessionHolder
    private lateinit var repository: SessionRepository
    private lateinit var audioPlayer: SessionAudioPlayer
    private val clock = Clock { 1_700_000_000_000L }

    private fun ex(id: String, isUnilateral: Boolean = false, totalTime: Int = 60) =
        Exercise(
            id = id,
            name = id,
            category = Category.HIPS,
            cues = emptyList(),
            isUnilateral = isUnilateral,
            isTimed = true,
            targetReps = null,
            secondsPerRep = null,
            totalTime = totalTime,
        )

    private fun item(id: String, seconds: Int = 60, isUnilateral: Boolean = false) =
        PlannedExercise(ex(id, isUnilateral, seconds), seconds, isForced = false)

    private fun plan(vararg items: PlannedExercise) =
        SessionPlan(items.toList(), items.sumOf { it.effectiveSeconds }, emptyMap())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        holder = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        audioPlayer = mockk(relaxed = true)
        coJustRun { holder.onSessionCompleted() }
        coJustRun { holder.ensureFresh(any()) }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun stubHolder(plan: SessionPlan) {
        coEvery { holder.state } returns MutableStateFlow(
            TodaySession(LocalDate.of(2026, 4, 23), plan)
        )
    }

    private fun viewModel(): SessionPlayerViewModel = SessionPlayerViewModel(
        holder = holder,
        repository = repository,
        audioPlayer = audioPlayer,
        clock = clock,
    )

    @Test
    fun `init emits Running at index 0 and fires start chime`() = runTest {
        stubHolder(plan(item("a", 30), item("b", 30)))
        val vm = viewModel()
        advanceUntilIdle()

        val running = vm.state.value as SessionPlayerUiState.Running
        assertEquals(0, running.currentIndex)
        assertEquals(Side.NONE, running.side)
        assertEquals(30, running.remainingSeconds)
        assertEquals(30, running.totalSecondsForPhase)
        assertTrue(!running.isPaused)
        coVerify { audioPlayer.playStart() }
    }

    @Test
    fun `tick decrements remainingSeconds`() = runTest {
        stubHolder(plan(item("a", 5)))
        val vm = viewModel()
        advanceUntilIdle()

        vm.tick()
        assertEquals(4, (vm.state.value as SessionPlayerUiState.Running).remainingSeconds)
        vm.tick()
        assertEquals(3, (vm.state.value as SessionPlayerUiState.Running).remainingSeconds)
    }

    @Test
    fun `paused tick is a no-op`() = runTest {
        stubHolder(plan(item("a", 5)))
        val vm = viewModel()
        advanceUntilIdle()

        vm.togglePause()
        vm.tick()
        vm.tick()

        val r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(5, r.remainingSeconds)
        assertTrue(r.isPaused)
    }

    @Test
    fun `tick rolling over advances to next exercise and fires start chime`() = runTest {
        stubHolder(plan(item("a", 1), item("b", 30)))
        coEvery { audioPlayer.playStart() } returns Unit
        val vm = viewModel()
        advanceUntilIdle()

        vm.tick() // 1 → 0 → advance

        val r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(1, r.currentIndex)
        assertEquals(30, r.remainingSeconds)
        coVerify(atLeast = 2) { audioPlayer.playStart() }
    }

    @Test
    fun `unilateral LEFT advances to RIGHT before next exercise`() = runTest {
        stubHolder(plan(item("a", 2, isUnilateral = true), item("b", 10)))
        val vm = viewModel()
        advanceUntilIdle()

        // 2s split: LEFT gets ceiling → 1s, RIGHT 1s.
        vm.tick() // LEFT 1 → 0 → switch to RIGHT
        var r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(0, r.currentIndex)
        assertEquals(Side.RIGHT, r.side)
        assertEquals(1, r.remainingSeconds)

        vm.tick() // RIGHT 1 → 0 → advance to b
        r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(1, r.currentIndex)
        assertEquals(Side.NONE, r.side)
    }

    @Test
    fun `skip jumps straight to next exercise`() = runTest {
        stubHolder(plan(item("a", 60), item("b", 60)))
        val vm = viewModel()
        advanceUntilIdle()

        vm.skip()
        val r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(1, r.currentIndex)
    }

    @Test
    fun `finishing the last exercise persists a session and transitions to Complete`() = runTest {
        val only = item("only", 1)
        stubHolder(plan(only))
        coEvery { repository.completeSession(any(), any()) } returns 1L
        coEvery { repository.currentStreakDays() } returns 7
        val vm = viewModel()
        advanceUntilIdle()

        vm.tick() // 1 → 0 → finish
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("expected Complete but was $state", state is SessionPlayerUiState.Complete)
        val c = state as SessionPlayerUiState.Complete
        assertEquals(1, c.areasStretched)
        assertEquals(0, c.totalMinutes) // 1s → 0 min (integer truncation)
        assertEquals(7, c.streakAfter)

        coVerify { repository.completeSession(any(), any()) }
        coVerify { audioPlayer.playEnd() }
        coVerify { holder.onSessionCompleted() }
    }

    @Test
    fun `double-tick at the final exercise does not persist the session twice`() = runTest {
        val only = item("only", 1)
        stubHolder(plan(only))
        coEvery { repository.completeSession(any(), any()) } returns 1L
        coEvery { repository.currentStreakDays() } returns 4
        val vm = viewModel()
        advanceUntilIdle()

        // First tick: 1 → 0 → finish. Second tick: would re-enter finish()
        // if state were still Running — it isn't, because finish flips
        // to Complete synchronously.
        vm.tick()
        vm.tick()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.completeSession(any(), any()) }
        coVerify(exactly = 1) { holder.onSessionCompleted() }
        assertTrue(vm.state.value is SessionPlayerUiState.Complete)
    }

    @Test
    fun `areasStretched counts distinct categories`() = runTest {
        val hipsOne = item("H01", 1)
        val hipsTwo = PlannedExercise(
            exercise = ex("H02").copy(category = Category.HIPS),
            effectiveSeconds = 1,
            isForced = false,
        )
        val neckOne = PlannedExercise(
            exercise = ex("N01").copy(category = Category.NECK),
            effectiveSeconds = 1,
            isForced = false,
        )
        stubHolder(plan(hipsOne, hipsTwo, neckOne))
        coEvery { repository.completeSession(any(), any()) } returns 2L
        coEvery { repository.currentStreakDays() } returns 3

        val vm = viewModel()
        advanceUntilIdle()
        // Skip through all three items.
        vm.skip(); vm.skip(); vm.skip()
        advanceUntilIdle()

        val c = vm.state.value as SessionPlayerUiState.Complete
        assertEquals(2, c.areasStretched) // HIPS + NECK
    }

    @Test
    fun `phaseSeconds bilateral returns full budget`() {
        val full = item("a", 60, isUnilateral = false)
        assertEquals(60, SessionPlayerViewModel.phaseSeconds(full, Side.NONE))
    }

    @Test
    fun `phaseSeconds unilateral splits with ceiling on LEFT`() {
        val uni = item("a", 61, isUnilateral = true)
        assertEquals(31, SessionPlayerViewModel.phaseSeconds(uni, Side.LEFT))
        assertEquals(30, SessionPlayerViewModel.phaseSeconds(uni, Side.RIGHT))
    }
}
