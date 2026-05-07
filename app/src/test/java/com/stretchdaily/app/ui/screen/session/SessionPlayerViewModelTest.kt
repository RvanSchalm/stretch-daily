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
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionPlayerViewModelTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
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
    fun `init lands in READY at index 0 with no chime`() = runTest {
        stubHolder(plan(item("a", 30), item("b", 30)))
        val vm = viewModel()
        runCurrent()

        val running = vm.state.value as SessionPlayerUiState.Running
        assertEquals(0, running.currentIndex)
        assertEquals(Side.NONE, running.side)
        assertEquals(30, running.remainingSeconds)
        assertEquals(30, running.totalSecondsForPhase)
        assertEquals(TimerPhase.READY, running.phase)
        coVerify(exactly = 0) { audioPlayer.playStart() }
        coVerify(exactly = 0) { audioPlayer.playEnd() }
    }

    @Test
    fun `start moves READY to RUNNING silently`() = runTest {
        stubHolder(plan(item("a", 5)))
        val vm = viewModel()
        runCurrent()

        vm.start()
        runCurrent()

        val r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(TimerPhase.RUNNING, r.phase)
        coVerify(exactly = 0) { audioPlayer.playStart() }
        coVerify(exactly = 0) { audioPlayer.playEnd() }

        // Pause to halt the 1 Hz coroutine before runTest's drain.
        vm.pause()
    }

    @Test
    fun `tick decrements only when phase is RUNNING`() = runTest {
        stubHolder(plan(item("a", 5)))
        val vm = viewModel()
        runCurrent()

        // READY: tick is a no-op.
        vm.tick()
        assertEquals(5, (vm.state.value as SessionPlayerUiState.Running).remainingSeconds)

        // Start → RUNNING. Now ticks count down.
        vm.start()
        vm.tick()
        assertEquals(4, (vm.state.value as SessionPlayerUiState.Running).remainingSeconds)
        vm.tick()
        assertEquals(3, (vm.state.value as SessionPlayerUiState.Running).remainingSeconds)

        vm.pause()
    }

    @Test
    fun `paused tick is a no-op`() = runTest {
        stubHolder(plan(item("a", 5)))
        val vm = viewModel()
        runCurrent()

        vm.start()
        vm.pause()
        vm.tick()
        vm.tick()

        val r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(5, r.remainingSeconds)
        assertEquals(TimerPhase.PAUSED, r.phase)

        // Resume so runTest's drain can finish the timer.
        vm.resume()
    }

    @Test
    fun `pause resume round-trips between RUNNING and PAUSED`() = runTest {
        stubHolder(plan(item("a", 5)))
        val vm = viewModel()
        runCurrent()

        vm.start()
        assertEquals(TimerPhase.RUNNING, (vm.state.value as SessionPlayerUiState.Running).phase)

        vm.pause()
        assertEquals(TimerPhase.PAUSED, (vm.state.value as SessionPlayerUiState.Running).phase)

        vm.resume()
        assertEquals(TimerPhase.RUNNING, (vm.state.value as SessionPlayerUiState.Running).phase)

        vm.pause()
    }

    @Test
    fun `tick at zero plays chime once and advances to next phase READY`() = runTest {
        stubHolder(plan(item("a", 1), item("b", 30)))
        val vm = viewModel()
        runCurrent()

        vm.start()
        vm.tick()  // 1 → 0 → chime + advance
        runCurrent()

        val r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(1, r.currentIndex)
        assertEquals(30, r.remainingSeconds)
        assertEquals(TimerPhase.READY, r.phase)
        coVerify(exactly = 1) { audioPlayer.playEnd() }
        coVerify(exactly = 0) { audioPlayer.playStart() }
    }

    @Test
    fun `next from READY advances silently`() = runTest {
        stubHolder(plan(item("a", 30), item("b", 30)))
        val vm = viewModel()
        runCurrent()

        vm.next()

        val r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(1, r.currentIndex)
        assertEquals(TimerPhase.READY, r.phase)
        coVerify(exactly = 0) { audioPlayer.playEnd() }
        coVerify(exactly = 0) { audioPlayer.playStart() }
    }

    @Test
    fun `unilateral LEFT phase end transitions to RIGHT READY needing its own start`() = runTest {
        stubHolder(plan(item("a", 2, isUnilateral = true), item("b", 10)))
        val vm = viewModel()
        runCurrent()

        // 2s budget split: LEFT gets 1s ceiling, RIGHT 1s.
        vm.start()
        vm.tick()  // LEFT 1 → 0 → chime + transition to RIGHT READY
        runCurrent()

        var r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(0, r.currentIndex)
        assertEquals(Side.RIGHT, r.side)
        assertEquals(1, r.remainingSeconds)
        assertEquals(TimerPhase.READY, r.phase)
        coVerify(exactly = 1) { audioPlayer.playEnd() }

        // RIGHT requires its own Start.
        vm.start()
        vm.tick()  // RIGHT 1 → 0 → chime + advance to b
        runCurrent()

        r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(1, r.currentIndex)
        assertEquals(Side.NONE, r.side)
        assertEquals(TimerPhase.READY, r.phase)
        coVerify(exactly = 2) { audioPlayer.playEnd() }
    }

    @Test
    fun `prev from RIGHT goes to LEFT READY of same exercise`() = runTest {
        stubHolder(plan(item("a", 60, isUnilateral = true)))
        val vm = viewModel()
        runCurrent()

        // Force into RIGHT side by ticking through LEFT.
        vm.start()
        repeat(31) { vm.tick() }   // LEFT 30s → 0 → switch to RIGHT 30s (with ceiling)
        runCurrent()
        var r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(Side.RIGHT, r.side)

        // Now prev should go back to LEFT READY.
        vm.prev()
        r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(Side.LEFT, r.side)
        assertEquals(TimerPhase.READY, r.phase)
    }

    @Test
    fun `final phase tick-end transitions to Complete with one chime total`() = runTest {
        val only = item("only", 1)
        stubHolder(plan(only))
        coEvery { repository.completeSession(any(), any()) } returns 1L
        coEvery { repository.currentStreakDays() } returns 7
        val vm = viewModel()
        runCurrent()

        vm.start()
        vm.tick()  // 1 → 0 → chime, advance, finish
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("expected Complete, was $state", state is SessionPlayerUiState.Complete)
        val c = state as SessionPlayerUiState.Complete
        assertEquals(1, c.areasStretched)
        assertEquals(0, c.totalMinutes)
        assertEquals(7, c.streakAfter)

        coVerify(exactly = 1) { audioPlayer.playEnd() }   // exactly one chime, not two
        coVerify { repository.completeSession(any(), any()) }
        coVerify { holder.onSessionCompleted() }
    }

    @Test
    fun `next from final phase silently completes session`() = runTest {
        val only = item("only", 30)
        stubHolder(plan(only))
        coEvery { repository.completeSession(any(), any()) } returns 1L
        coEvery { repository.currentStreakDays() } returns 4
        val vm = viewModel()
        runCurrent()

        vm.next()  // skip silently to end → finish
        advanceUntilIdle()

        assertTrue(vm.state.value is SessionPlayerUiState.Complete)
        coVerify(exactly = 0) { audioPlayer.playEnd() }   // silent completion
        coVerify(exactly = 1) { repository.completeSession(any(), any()) }
    }

    @Test
    fun `areasStretched counts distinct categories across the plan`() = runTest {
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
        runCurrent()

        vm.next(); vm.next(); vm.next()
        advanceUntilIdle()

        val c = vm.state.value as SessionPlayerUiState.Complete
        assertEquals(2, c.areasStretched)
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
