package com.stretchdaily.app.ui.screen.log

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BenchmarkLogViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: BenchmarkRepository
    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, zone)
        .toInstant().toEpochMilli()
    private val clock = Clock { now }

    private val sitReach = Benchmark(
        id = "BM_SIT_AND_REACH",
        name = "Sit and Reach",
        category = Category.SPINE,
        description = "Reach forward, legs straight.",
        unit = "cm",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = emptyMap(),
    )

    private val atgSquat = Benchmark(
        id = "BM_ATG_SPLIT_SQUAT",
        name = "ATG Split Squat",
        category = Category.KNEES,
        description = "Depth control under load.",
        unit = "",
        inputType = BenchmarkInputType.CATEGORICAL,
        tierRanges = emptyMap(),
    )

    private fun log(
        id: Long,
        benchmarkId: String,
        tier: FlexibilityTier,
        daysAgo: Long,
        raw: String = tier.name,
    ) = BenchmarkLog(
        id = id,
        benchmarkId = benchmarkId,
        rawValue = raw,
        resolvedTier = tier,
        loggedAt = Instant.ofEpochMilli(now)
            .minusSeconds(daysAgo * 86_400).toEpochMilli(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
    }

    @After fun teardown() = Dispatchers.resetMain()

    private fun viewModel(): BenchmarkLogViewModel =
        BenchmarkLogViewModel(repo, clock)

    @Test
    fun `rows group by category in enum order`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(atgSquat, sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())

        val vm = viewModel()
        advanceUntilIdle()

        val groups = vm.state.value.groups
        // SPINE comes before KNEES in Category enum.
        assertEquals(listOf(Category.SPINE, Category.KNEES), groups.map { it.category })
    }

    @Test
    fun `row exposes latest log and sparkline values in window`() = runTest(testDispatcher) {
        val older = log(1, sitReach.id, FlexibilityTier.STIFF, daysAgo = 90)
        val recent = log(2, sitReach.id, FlexibilityTier.AVERAGE, daysAgo = 20)
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(listOf(recent))
        coEvery { repo.observeLogsFor(sitReach.id) } returns flowOf(listOf(older, recent))

        val vm = viewModel()
        advanceUntilIdle()

        val row = vm.state.value.groups.single().rows.single()
        assertEquals(recent.id, row.latestLog?.id)
        assertEquals(listOf(0.0, 0.5), row.sparkline) // oldest→newest
        // History is newest-first for the expanded table.
        assertEquals(listOf(recent.id, older.id), row.history.map { it.id })
    }

    @Test
    fun `overdue flag is true when latest log predates this calendar month`() = runTest(testDispatcher) {
        // today is June 15, 2026. A log from May 20 → predates June → overdue.
        val lastMonthLog = log(5, sitReach.id, FlexibilityTier.AVERAGE, daysAgo = 26)
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(listOf(lastMonthLog))
        coEvery { repo.observeLogsFor(sitReach.id) } returns flowOf(listOf(lastMonthLog))

        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.groups.single().rows.single().isOverdue)
    }

    @Test
    fun `overdue flag is false when a log exists this calendar month`() = runTest(testDispatcher) {
        val thisMonthLog = log(6, sitReach.id, FlexibilityTier.AVERAGE, daysAgo = 3)
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(listOf(thisMonthLog))
        coEvery { repo.observeLogsFor(sitReach.id) } returns flowOf(listOf(thisMonthLog))

        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.state.value.groups.single().rows.single().isOverdue)
    }

    @Test
    fun `overdue flag is true when user has never logged`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(sitReach.id) } returns flowOf(emptyList())

        val vm = viewModel()
        advanceUntilIdle()

        val row = vm.state.value.groups.single().rows.single()
        assertNull(row.latestLog)
        assertTrue(row.isOverdue)
    }

    @Test
    fun `onRowTapped toggles expansion`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.expandedRowId)
        vm.onRowTapped(sitReach.id)
        assertEquals(sitReach.id, vm.state.value.expandedRowId)
        vm.onRowTapped(sitReach.id)
        assertNull(vm.state.value.expandedRowId)
    }

    @Test
    fun `onLogPressed then onDismissSheet flips the sheet visibility`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.onLogPressed(sitReach.id)
        val visible = vm.state.value.sheet
        assertTrue(visible is LogSheetState.Visible)
        assertEquals(sitReach.id, (visible as LogSheetState.Visible).benchmark.id)

        vm.onDismissSheet()
        assertTrue(vm.state.value.sheet is LogSheetState.Hidden)
    }

    @Test
    fun `onSubmit numeric calls repository logNumeric then closes sheet`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        coEvery { repo.logNumeric(sitReach.id, "12.5") } returns Result.success(1L)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onLogPressed(sitReach.id)
        vm.onSubmit(rawValue = "12.5", tierOverride = null)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.logNumeric(sitReach.id, "12.5") }
        assertTrue(vm.state.value.sheet is LogSheetState.Hidden)
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun `onSubmit categorical calls repository logCategorical`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(atgSquat))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        coEvery { repo.logCategorical(atgSquat.id, FlexibilityTier.FLEXIBLE) } returns 9L
        val vm = viewModel()
        advanceUntilIdle()

        vm.onLogPressed(atgSquat.id)
        vm.onSubmit(rawValue = FlexibilityTier.FLEXIBLE.name, tierOverride = FlexibilityTier.FLEXIBLE)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.logCategorical(atgSquat.id, FlexibilityTier.FLEXIBLE) }
        assertTrue(vm.state.value.sheet is LogSheetState.Hidden)
    }

    @Test
    fun `numeric submit surfacing parse failure sets error message and keeps sheet open`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        coEvery { repo.logNumeric(sitReach.id, "oops") } returns
            Result.failure(IllegalArgumentException("Not a number: oops"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onLogPressed(sitReach.id)
        vm.onSubmit(rawValue = "oops", tierOverride = null)
        advanceUntilIdle()

        assertTrue(vm.state.value.sheet is LogSheetState.Visible)
        assertEquals("Not a number: oops", vm.state.value.errorMessage)
    }

    @Test
    fun `onDeleteLog delegates to repository`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDeleteLog(42L)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.deleteLog(42L) }
    }
}
