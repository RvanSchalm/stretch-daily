package com.stretchdaily.app.ui.screen.dashboard

import com.stretchdaily.app.core.datastore.SettingsDataStore
import com.stretchdaily.app.core.engine.CategoryWeightCalculator
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.core.session.TodaySession
import com.stretchdaily.app.core.session.TodaySessionHolder
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import com.stretchdaily.app.data.SessionRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers the 7-flow composition in [DashboardViewModel]. Each test stubs
 * the collaborators with `flowOf` so `stateIn(Eagerly)` completes
 * synchronously under [UnconfinedTestDispatcher].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var sessionRepository: SessionRepository
    private lateinit var benchmarkRepository: BenchmarkRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var todaySessionHolder: TodaySessionHolder
    private val calculator = CategoryWeightCalculator()
    private val clock = Clock { 1_700_000_000_000L }

    private val hipsEx = Exercise(
        id = "H01",
        name = "Pigeon",
        category = Category.HIPS,
        cues = emptyList(),
        isUnilateral = false,
        isTimed = true,
        targetReps = null,
        secondsPerRep = null,
        totalTime = 60,
        lastPerformed = null,
    )

    private val planItem = PlannedExercise(hipsEx, effectiveSeconds = 60, isForced = false)

    private val plan = SessionPlan(
        items = listOf(planItem),
        totalSeconds = 60,
        categoryWeights = mapOf(Category.HIPS to FlexibilityTier.STIFF.weight),
    )

    private val benchmark = Benchmark(
        id = "BM_SIT_AND_REACH",
        name = "Sit and Reach",
        category = Category.SPINE,
        description = "",
        unit = "cm",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = emptyMap(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk(relaxed = true)
        benchmarkRepository = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)
        todaySessionHolder = mockk(relaxed = true)

        coEvery { sessionRepository.streakFlow(any()) } returns flowOf(4)
        coEvery { sessionRepository.weeklyFlow(any()) } returns flowOf(
            setOf(LocalDate.of(2023, 11, 13), LocalDate.of(2023, 11, 14))
        )
        coEvery { sessionRepository.totalsFlow } returns flowOf(
            SessionRepository.Totals(sessions = 14, totalMinutes = 120)
        )
        coEvery { benchmarkRepository.getAllBenchmarks() } returns listOf(benchmark)
        coEvery { benchmarkRepository.getLatestLogs() } returns emptyList()
        coEvery { benchmarkRepository.overdueFlow(any()) } returns flowOf(listOf(benchmark))
        coEvery { benchmarkRepository.nextDueFlow(any()) } returns flowOf(
            BenchmarkRepository.BenchmarkWithDueDate(benchmark, daysUntilDue = 4)
        )
        coEvery { settingsDataStore.benchmarkBannerEnabled } returns flowOf(false)
        coEvery { todaySessionHolder.state } returns MutableStateFlow(
            TodaySession(LocalDate.of(2023, 11, 15), plan)
        )
        coJustRun { todaySessionHolder.ensureFresh(any()) }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): DashboardViewModel = DashboardViewModel(
        sessionRepository = sessionRepository,
        benchmarkRepository = benchmarkRepository,
        settingsDataStore = settingsDataStore,
        todaySessionHolder = todaySessionHolder,
        categoryWeightCalculator = calculator,
        clock = clock,
    )

    @Test
    fun `Loaded state composes flows into a single UiState`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(4, state.streakDays)
        assertEquals(4, state.kpis.streakDays)
        assertEquals(14, state.kpis.totalSessions)
        assertEquals(120, state.kpis.totalMinutes)
        assertEquals(4, state.kpis.nextBenchmarkDays)
        assertEquals(1, state.plannedExercises.size)
        assertEquals(1, state.plannedMinutes) // 60 s / 60 = 1 minute
        assertEquals(Category.HIPS, state.extraFocus)
        // Banner disabled by preference.
        assertTrue(state.banner is BannerState.Hidden)
    }

    @Test
    fun `banner surfaces when preference is true and at least one benchmark is overdue`() = runTest {
        coEvery { settingsDataStore.benchmarkBannerEnabled } returns flowOf(true)
        val vm = viewModel()
        advanceUntilIdle()

        val banner = vm.state.value.banner
        assertTrue(banner is BannerState.Visible)
        val visible = banner as BannerState.Visible
        assertEquals(1, visible.overdueCount)
        assertEquals("BM_SIT_AND_REACH", visible.nextBenchmark?.id)
    }

    @Test
    fun `banner stays hidden when preference is true but nothing is overdue`() = runTest {
        coEvery { settingsDataStore.benchmarkBannerEnabled } returns flowOf(true)
        coEvery { benchmarkRepository.overdueFlow(any()) } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.banner is BannerState.Hidden)
    }

    @Test
    fun `ensureFresh is invoked on init`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        io.mockk.coVerify(exactly = 1) { todaySessionHolder.ensureFresh(any()) }
        // Avoid unused-var warnings.
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `extraFocus resolves to the stiffest category`() = runTest {
        // Two categories in the weight map — HIPS at STIFF (3.0) and SPINE at
        // AVERAGE (2.0). Stiffest has the highest weight.
        val spineEx = hipsEx.copy(id = "SP01", category = Category.SPINE)
        val planWithBoth = SessionPlan(
            items = listOf(planItem, PlannedExercise(spineEx, 60, false)),
            totalSeconds = 120,
            categoryWeights = mapOf(
                Category.HIPS to FlexibilityTier.STIFF.weight,
                Category.SPINE to FlexibilityTier.AVERAGE.weight,
            ),
        )
        coEvery { todaySessionHolder.state } returns MutableStateFlow(
            TodaySession(LocalDate.of(2023, 11, 15), planWithBoth)
        )

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(Category.HIPS, vm.state.value.extraFocus)
    }
}
