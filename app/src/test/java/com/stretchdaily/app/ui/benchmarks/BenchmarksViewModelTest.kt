package com.stretchdaily.app.ui.benchmarks

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.data.BenchmarkRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BenchmarksViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: BenchmarkRepository
    private lateinit var benchmarksFlow: MutableStateFlow<List<Benchmark>>
    private lateinit var latestLogsFlow: MutableStateFlow<List<BenchmarkLog>>

    private val numericBenchmark = Benchmark(
        id = "BM_KNEE_TO_WALL",
        name = "Knee-to-Wall",
        category = Category.ANKLES,
        description = "Ankle dorsiflexion test",
        unit = "cm",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = emptyMap(),
    )

    private val categoricalBenchmark = Benchmark(
        id = "BM_ATG_SPLIT_SQUAT",
        name = "ATG Split Squat",
        category = Category.KNEES,
        description = "Knee-over-toe depth",
        unit = "Tier",
        inputType = BenchmarkInputType.CATEGORICAL,
        tierRanges = emptyMap(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        benchmarksFlow = MutableStateFlow(emptyList())
        latestLogsFlow = MutableStateFlow(emptyList())
        every { repository.observeAllBenchmarks() } returns benchmarksFlow
        every { repository.observeLatestLogs() } returns latestLogsFlow
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state transitions to Loaded with catalog and no logs`() = runTest {
        benchmarksFlow.value = listOf(numericBenchmark, categoricalBenchmark)
        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("expected Loaded, was $state", state is BenchmarksUiState.Loaded)
        val loaded = state as BenchmarksUiState.Loaded
        assertEquals(2, loaded.rows.size)
        assertNull(loaded.rows[0].latestLog)
    }

    @Test
    fun `state pairs latest logs with benchmarks`() = runTest {
        benchmarksFlow.value = listOf(numericBenchmark)
        latestLogsFlow.value = listOf(
            BenchmarkLog(
                id = 42,
                benchmarkId = "BM_KNEE_TO_WALL",
                rawValue = "12",
                resolvedTier = FlexibilityTier.FLEXIBLE,
                loggedAt = 1_700_000_000_000L,
            )
        )

        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()

        val loaded = vm.state.value as BenchmarksUiState.Loaded
        assertNotNull(loaded.rows[0].latestLog)
        assertEquals("12", loaded.rows[0].latestLog?.rawValue)
    }

    @Test
    fun `state recomputes when latest logs flow emits a new value`() = runTest {
        // Start with one log present, then simulate a "delete all data"
        // wipe by emitting an empty list. The card should drop its
        // latestLog without any explicit refresh call.
        benchmarksFlow.value = listOf(numericBenchmark)
        latestLogsFlow.value = listOf(
            BenchmarkLog(
                id = 42,
                benchmarkId = "BM_KNEE_TO_WALL",
                rawValue = "12",
                resolvedTier = FlexibilityTier.FLEXIBLE,
                loggedAt = 1_700_000_000_000L,
            )
        )

        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()
        assertNotNull(
            (vm.state.value as BenchmarksUiState.Loaded).rows[0].latestLog
        )

        latestLogsFlow.value = emptyList()
        advanceUntilIdle()

        val loadedAfter = vm.state.value as BenchmarksUiState.Loaded
        assertNull(
            "card should drop its latest log after the flow re-emits empty",
            loadedAfter.rows[0].latestLog,
        )
    }

    @Test
    fun `openLogDialog seeds a fresh numeric dialog`() = runTest {
        benchmarksFlow.value = listOf(numericBenchmark, categoricalBenchmark)
        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()

        vm.openLogDialog(numericBenchmark)

        val dialog = vm.dialog.value
        assertNotNull(dialog)
        assertEquals(numericBenchmark.id, dialog!!.benchmark.id)
        assertEquals("", dialog.rawInput)
        assertNull(dialog.editingLogId)
    }

    @Test
    fun `submitDialog numeric success closes the dialog`() = runTest {
        benchmarksFlow.value = listOf(numericBenchmark)
        coEvery { repository.logNumeric("BM_KNEE_TO_WALL", "12.5") } returns Result.success(1L)

        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()
        vm.openLogDialog(numericBenchmark)
        vm.onRawInputChange("12.5")
        vm.submitDialog()
        advanceUntilIdle()

        assertNull(vm.dialog.value)
        coVerify { repository.logNumeric("BM_KNEE_TO_WALL", "12.5") }
    }

    @Test
    fun `submitDialog numeric failure keeps dialog open and stores error`() = runTest {
        benchmarksFlow.value = listOf(numericBenchmark)
        coEvery { repository.logNumeric("BM_KNEE_TO_WALL", "oops") } returns
            Result.failure(IllegalArgumentException("Not a number: oops"))

        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()
        vm.openLogDialog(numericBenchmark)
        vm.onRawInputChange("oops")
        vm.submitDialog()
        advanceUntilIdle()

        val dialog = vm.dialog.value
        assertNotNull(dialog)
        assertEquals("Not a number: oops", dialog?.error)
    }

    @Test
    fun `submitDialog rejects empty numeric input without hitting the repo`() = runTest {
        benchmarksFlow.value = listOf(numericBenchmark)
        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()
        vm.openLogDialog(numericBenchmark)
        vm.submitDialog()
        advanceUntilIdle()

        assertEquals("Enter a value", vm.dialog.value?.error)
        coVerify(exactly = 0) { repository.logNumeric(any(), any()) }
    }

    @Test
    fun `submitDialog categorical success calls logCategorical with selected tier`() = runTest {
        benchmarksFlow.value = listOf(categoricalBenchmark)
        val tierSlot = slot<FlexibilityTier>()
        coEvery {
            repository.logCategorical("BM_ATG_SPLIT_SQUAT", capture(tierSlot))
        } returns 1L

        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()
        vm.openLogDialog(categoricalBenchmark)
        vm.onTierSelect(FlexibilityTier.FLEXIBLE)
        vm.submitDialog()
        advanceUntilIdle()

        assertEquals(FlexibilityTier.FLEXIBLE, tierSlot.captured)
        assertNull(vm.dialog.value)
    }

    @Test
    fun `submitDialog rejects categorical without selection`() = runTest {
        benchmarksFlow.value = listOf(categoricalBenchmark)
        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()
        vm.openLogDialog(categoricalBenchmark)
        vm.submitDialog()
        advanceUntilIdle()

        assertEquals("Pick a tier", vm.dialog.value?.error)
    }

    @Test
    fun `openEditDialog pre-fills the existing raw value`() = runTest {
        benchmarksFlow.value = listOf(numericBenchmark)
        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()

        val existing = BenchmarkLog(
            id = 7,
            benchmarkId = "BM_KNEE_TO_WALL",
            rawValue = "8.0",
            resolvedTier = FlexibilityTier.AVERAGE,
            loggedAt = 1_700_000_000_000L,
        )
        vm.openEditDialog(numericBenchmark, existing)

        val dialog = vm.dialog.value
        assertNotNull(dialog)
        assertEquals(7L, dialog?.editingLogId)
        assertEquals("8.0", dialog?.rawInput)
    }

    @Test
    fun `submitDialog edit numeric calls updateLog via repository`() = runTest {
        benchmarksFlow.value = listOf(numericBenchmark)
        val existing = BenchmarkLog(
            id = 7,
            benchmarkId = "BM_KNEE_TO_WALL",
            rawValue = "8.0",
            resolvedTier = FlexibilityTier.AVERAGE,
            loggedAt = 1_700_000_000_000L,
        )
        coEvery { repository.getLog(7) } returns existing
        coEvery {
            repository.updateLog(
                existing = existing,
                rawValue = "10.5",
                tierOverride = null,
                benchmarkInputType = BenchmarkInputType.NUMERIC,
            )
        } returns Result.success(Unit)

        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()
        vm.openEditDialog(numericBenchmark, existing)
        vm.onRawInputChange("10.5")
        vm.submitDialog()
        advanceUntilIdle()

        coVerify {
            repository.updateLog(
                existing = existing,
                rawValue = "10.5",
                tierOverride = null,
                benchmarkInputType = BenchmarkInputType.NUMERIC,
            )
        }
        assertNull(vm.dialog.value)
    }

    @Test
    fun `deleteLog forwards to repository`() = runTest {
        benchmarksFlow.value = listOf(numericBenchmark)
        coEvery { repository.deleteLog(99) } just Runs

        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()
        vm.deleteLog(
            BenchmarkLog(
                id = 99,
                benchmarkId = "BM_KNEE_TO_WALL",
                rawValue = "1",
                resolvedTier = FlexibilityTier.STIFF,
                loggedAt = 0L,
            )
        )
        advanceUntilIdle()

        coVerify { repository.deleteLog(99) }
    }

    @Test
    fun `state catches upstream errors and exposes Error state`() = runTest {
        every { repository.observeAllBenchmarks() } returns flow {
            throw IllegalStateException("boom")
        }

        val vm = BenchmarksViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("expected Error, was $state", state is BenchmarksUiState.Error)
        assertEquals("boom", (state as BenchmarksUiState.Error).message)
    }
}
