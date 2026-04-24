package com.stretchdaily.app.ui.screen.session

import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.session.TodaySession
import com.stretchdaily.app.core.session.TodaySessionHolder
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
class SessionOverviewViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var holder: TodaySessionHolder
    private lateinit var exerciseDao: ExerciseDao

    private fun ex(id: String, cat: Category = Category.HIPS) = Exercise(
        id = id,
        name = id,
        category = cat,
        cues = emptyList(),
        isUnilateral = false,
        isTimed = true,
        targetReps = null,
        secondsPerRep = null,
        totalTime = 60,
    )

    private fun planItem(id: String, cat: Category = Category.HIPS) =
        PlannedExercise(ex(id, cat), effectiveSeconds = 60, isForced = false)

    private val plan = SessionPlan(
        items = listOf(planItem("H01"), planItem("H02"), planItem("N01", Category.NECK)),
        totalSeconds = 180,
        categoryWeights = emptyMap(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        holder = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        coEvery { holder.state } returns MutableStateFlow(
            TodaySession(LocalDate.of(2026, 4, 23), plan)
        )
        coJustRun { holder.ensureFresh(any()) }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun vm(): SessionOverviewViewModel =
        SessionOverviewViewModel(holder, exerciseDao)

    @Test
    fun `state maps holder plan to Loaded with minute total`() = runTest {
        val viewModel = vm()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertEquals(3, s.items.size)
        assertEquals(3, s.planMinutes) // 180s / 60 = 3 min
        assertTrue(s.swap is SwapSheetState.Hidden)
    }

    @Test
    fun `onExerciseTap loads same-category candidates excluding plan exercises`() = runTest {
        coEvery { exerciseDao.getByCategory(Category.HIPS) } returns listOf(
            ex("H01"), ex("H02"), ex("H03"), ex("H04"),
        )

        val viewModel = vm()
        advanceUntilIdle()
        viewModel.onExerciseTap("H01")
        advanceUntilIdle()

        val s = viewModel.state.value
        assertTrue(s.swap is SwapSheetState.Visible)
        val sheet = s.swap as SwapSheetState.Visible
        assertEquals("H01", sheet.oldItem.exercise.id)
        // H01 is the current item, H02 is already in the plan → both excluded.
        assertEquals(listOf("H03", "H04"), sheet.candidates.map { it.id })
    }

    @Test
    fun `onSwapSelect delegates to holder and closes the sheet`() = runTest {
        coEvery { exerciseDao.getByCategory(Category.HIPS) } returns listOf(
            ex("H01"), ex("H03"),
        )
        val viewModel = vm()
        advanceUntilIdle()
        viewModel.onExerciseTap("H01")
        advanceUntilIdle()
        viewModel.onSwapSelect("H03")
        advanceUntilIdle()

        coVerify { holder.swap("H01", "H03") }
        assertTrue(viewModel.state.value.swap is SwapSheetState.Hidden)
    }

    @Test
    fun `onDismissSwap closes the sheet without calling holder`() = runTest {
        coEvery { exerciseDao.getByCategory(Category.HIPS) } returns listOf(ex("H01"))
        val viewModel = vm()
        advanceUntilIdle()
        viewModel.onExerciseTap("H01")
        advanceUntilIdle()
        viewModel.onDismissSwap()

        assertTrue(viewModel.state.value.swap is SwapSheetState.Hidden)
        coVerify(exactly = 0) { holder.swap(any(), any()) }
    }
}
