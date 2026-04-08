package com.stretchdaily.app.ui.home

import com.stretchdaily.app.core.engine.CategoryWeightCalculator
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.data.BenchmarkRepository
import com.stretchdaily.app.data.SessionRepository
import io.mockk.coEvery
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var benchmarkRepository: BenchmarkRepository
    private lateinit var sessionRepository: SessionRepository
    private val calculator = CategoryWeightCalculator()

    private val kneeToWall = Benchmark(
        id = "BM_KNEE_TO_WALL",
        name = "Knee-to-Wall",
        category = Category.ANKLES,
        description = "",
        unit = "cm",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = emptyMap(),
    )

    private val cervicalRotation = Benchmark(
        id = "BM_CERVICAL_ROTATION",
        name = "Cervical Rotation",
        category = Category.NECK,
        description = "",
        unit = "deg",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = emptyMap(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        benchmarkRepository = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private suspend fun stubDefaults(
        due: Boolean = false,
        streak: Int = 0,
        total: Int = 0,
        weekly: Int = 0,
        lastAt: Long? = null,
        benchmarks: List<Benchmark> = emptyList(),
        logs: List<BenchmarkLog> = emptyList(),
    ) {
        coEvery { benchmarkRepository.isBenchmarksDue() } returns due
        coEvery { benchmarkRepository.getAllBenchmarks() } returns benchmarks
        coEvery { benchmarkRepository.getLatestLogs() } returns logs
        coEvery { sessionRepository.currentStreakDays() } returns streak
        coEvery { sessionRepository.totalSessions() } returns total
        coEvery { sessionRepository.sessionsThisWeek() } returns weekly
        coEvery { sessionRepository.lastCompletedAt() } returns lastAt
    }

    @Test
    fun `refresh fans out to all sources and exposes a Loaded state`() = runTest {
        stubDefaults(
            due = true,
            streak = 4,
            total = 12,
            weekly = 3,
            lastAt = 1_700_000_000_000L,
            benchmarks = listOf(kneeToWall),
            logs = listOf(
                BenchmarkLog(
                    id = 1,
                    benchmarkId = "BM_KNEE_TO_WALL",
                    rawValue = "12",
                    resolvedTier = FlexibilityTier.FLEXIBLE,
                    loggedAt = 0L,
                )
            ),
        )

        val vm = HomeViewModel(benchmarkRepository, sessionRepository, calculator)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertTrue(state.benchmarksDue)
        assertEquals(4, state.streakDays)
        assertEquals(12, state.totalSessions)
        assertEquals(3, state.sessionsThisWeek)
        assertEquals(1_700_000_000_000L, state.lastSessionAt)
        // Heatmap is one row per Category enum entry.
        assertEquals(Category.entries.size, state.heatmap.size)
    }

    @Test
    fun `heatmap defaults all categories to AVERAGE when no logs`() = runTest {
        stubDefaults(benchmarks = listOf(kneeToWall, cervicalRotation), logs = emptyList())

        val vm = HomeViewModel(benchmarkRepository, sessionRepository, calculator)
        advanceUntilIdle()

        val state = vm.state.value
        // Every row should be AVERAGE (weight 2.0) and therefore not stiff.
        state.heatmap.forEach { row ->
            assertEquals(FlexibilityTier.AVERAGE, row.tier)
            assertEquals(FlexibilityTier.AVERAGE.weight, row.weight, 0.0001)
            assertFalse("${row.category} should not be stiff", row.isStiff)
        }
    }

    @Test
    fun `heatmap row reflects a stiff log for its category`() = runTest {
        stubDefaults(
            benchmarks = listOf(kneeToWall),
            logs = listOf(
                BenchmarkLog(
                    id = 1,
                    benchmarkId = "BM_KNEE_TO_WALL",
                    rawValue = "2",
                    resolvedTier = FlexibilityTier.STIFF,
                    loggedAt = 0L,
                )
            ),
        )

        val vm = HomeViewModel(benchmarkRepository, sessionRepository, calculator)
        advanceUntilIdle()

        val ankles = vm.state.value.heatmap.first { it.category == Category.ANKLES }
        assertEquals(FlexibilityTier.STIFF, ankles.tier)
        assertEquals(FlexibilityTier.STIFF.weight, ankles.weight, 0.0001)
        assertTrue(ankles.isStiff)
    }

    @Test
    fun `heatmap snaps fractional weight to closest tier`() = runTest {
        // Two logs in the same category — STIFF (3.0) and AVERAGE (2.0)
        // average to 2.5, which is BELOW_AVERAGE.
        val knee2 = kneeToWall.copy(id = "BM_THOMAS_TEST", category = Category.ANKLES)
        stubDefaults(
            benchmarks = listOf(kneeToWall, knee2),
            logs = listOf(
                BenchmarkLog(
                    id = 1,
                    benchmarkId = "BM_KNEE_TO_WALL",
                    rawValue = "2",
                    resolvedTier = FlexibilityTier.STIFF,
                    loggedAt = 0L,
                ),
                BenchmarkLog(
                    id = 2,
                    benchmarkId = "BM_THOMAS_TEST",
                    rawValue = "0",
                    resolvedTier = FlexibilityTier.AVERAGE,
                    loggedAt = 0L,
                ),
            ),
        )

        val vm = HomeViewModel(benchmarkRepository, sessionRepository, calculator)
        advanceUntilIdle()

        val ankles = vm.state.value.heatmap.first { it.category == Category.ANKLES }
        assertEquals(FlexibilityTier.BELOW_AVERAGE, ankles.tier)
        assertEquals(2.5, ankles.weight, 0.0001)
        assertTrue("BELOW_AVERAGE should be flagged stiff", ankles.isStiff)
    }

    @Test
    fun `tierFromWeight snaps to closest enum value`() {
        assertEquals(FlexibilityTier.STIFF, HomeViewModel.tierFromWeight(2.9))
        assertEquals(FlexibilityTier.BELOW_AVERAGE, HomeViewModel.tierFromWeight(2.5))
        assertEquals(FlexibilityTier.AVERAGE, HomeViewModel.tierFromWeight(2.1))
        assertEquals(FlexibilityTier.FLEXIBLE, HomeViewModel.tierFromWeight(1.5))
        assertEquals(FlexibilityTier.VERY_FLEXIBLE, HomeViewModel.tierFromWeight(1.1))
    }

    @Test
    fun `init triggers an immediate refresh`() = runTest {
        stubDefaults(streak = 9, total = 50)
        val vm = HomeViewModel(benchmarkRepository, sessionRepository, calculator)
        advanceUntilIdle()

        assertNotNull(vm.state.value)
        assertEquals(9, vm.state.value.streakDays)
        assertEquals(50, vm.state.value.totalSessions)
    }
}
