package com.stretchdaily.app.ui.screen.analytics

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant().toEpochMilli()

    private fun numeric(id: String, cat: Category, name: String = id): Benchmark = Benchmark(
        id = id,
        name = name,
        category = cat,
        description = "",
        unit = "°",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = emptyMap(),
    )

    private fun log(id: String, raw: String, date: Long): BenchmarkLog = BenchmarkLog(
        benchmarkId = id,
        rawValue = raw,
        resolvedTier = FlexibilityTier.AVERAGE,
        loggedAt = date,
    )

    private val catalog = listOf(
        numeric("BM_CERVICAL_ROTATION", Category.NECK, "Cervical Rotation"),
        numeric("BM_KNEE_TO_WALL", Category.ANKLES, "Knee-to-wall"),
        numeric("BM_SIT_AND_REACH", Category.HAMSTRINGS, "Sit and Reach"),
    )

    private lateinit var repo: BenchmarkRepository
    private lateinit var testDispatcher: UnconfinedTestDispatcher

    @Before
    fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(now: Long = midday(2026, 4, 23)): AnalyticsViewModel =
        AnalyticsViewModel(repo, Clock { now }, zoneId = zone)

    @Test
    fun `empty catalog yields empty state`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        assertEquals(emptyList<BenchmarkAnalyticsCardState>(), vm.state.value.cards)
        assertEquals(emptyList<Category>(), vm.state.value.allCategories)
        assertNull(vm.state.value.filter)
    }

    @Test
    fun `cards are emitted in catalog order regardless of log presence`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        assertEquals(
            listOf("BM_CERVICAL_ROTATION", "BM_KNEE_TO_WALL", "BM_SIT_AND_REACH"),
            vm.state.value.cards.map { it.benchmark.id },
        )
        vm.state.value.cards.forEach { card ->
            assertNull(card.latestRawValue)
            assertNull(card.delta)
            assertTrue(card.series.points.isEmpty())
        }
    }

    @Test
    fun `allCategories dedupes and preserves catalog order`() = runTest(testDispatcher) {
        val extended = catalog + numeric("BM_OTHER_NECK", Category.NECK)
        coEvery { repo.observeAllBenchmarks() } returns flowOf(extended)
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        assertEquals(
            listOf(Category.NECK, Category.ANKLES, Category.HAMSTRINGS),
            vm.state.value.allCategories,
        )
    }

    @Test
    fun `latestRawValue is newest log's raw value`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor("BM_CERVICAL_ROTATION") } returns flowOf(
            listOf(
                log("BM_CERVICAL_ROTATION", "60", midday(2025, 10, 23)),
                log("BM_CERVICAL_ROTATION", "72", midday(2026, 4, 10)),
            )
        )
        coEvery { repo.observeLogsFor("BM_KNEE_TO_WALL") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_SIT_AND_REACH") } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        val cervical = vm.state.value.cards.first { it.benchmark.id == "BM_CERVICAL_ROTATION" }
        assertEquals("72", cervical.latestRawValue)
    }

    @Test
    fun `delta is populated when logs span 6 months on ascending benchmark`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor("BM_CERVICAL_ROTATION") } returns flowOf(
            listOf(
                log("BM_CERVICAL_ROTATION", "60", midday(2025, 10, 23)),
                log("BM_CERVICAL_ROTATION", "72", midday(2026, 4, 10)),
            )
        )
        coEvery { repo.observeLogsFor("BM_KNEE_TO_WALL") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_SIT_AND_REACH") } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        val cervical = vm.state.value.cards.first { it.benchmark.id == "BM_CERVICAL_ROTATION" }
        assertNotNull(cervical.delta)
        assertEquals(12.0, cervical.delta!!.rawDelta, 0.001)
        assertTrue(cervical.delta!!.improved)
    }

    @Test
    fun `delta flips improved flag for descending benchmark`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor("BM_CERVICAL_ROTATION") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_KNEE_TO_WALL") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_SIT_AND_REACH") } returns flowOf(
            listOf(
                log("BM_SIT_AND_REACH", "15", midday(2025, 10, 23)),
                log("BM_SIT_AND_REACH", "-5", midday(2026, 4, 10)),
            )
        )
        val vm = vm()
        advanceUntilIdle()
        val sar = vm.state.value.cards.first { it.benchmark.id == "BM_SIT_AND_REACH" }
        assertNotNull(sar.delta)
        assertEquals(-20.0, sar.delta!!.rawDelta, 0.001)
        assertTrue(sar.delta!!.improved)
    }

    @Test
    fun `series emits a two-point series for two logs`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor("BM_CERVICAL_ROTATION") } returns flowOf(
            listOf(
                log("BM_CERVICAL_ROTATION", "60", midday(2025, 10, 23)),
                log("BM_CERVICAL_ROTATION", "72", midday(2026, 4, 10)),
            )
        )
        coEvery { repo.observeLogsFor("BM_KNEE_TO_WALL") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_SIT_AND_REACH") } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        val cervical = vm.state.value.cards.first { it.benchmark.id == "BM_CERVICAL_ROTATION" }
        assertEquals(2, cervical.series.points.size)
    }

    @Test
    fun `onFilterChanged sets filter`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        vm.onFilterChanged(Category.HAMSTRINGS)
        advanceUntilIdle()
        assertEquals(Category.HAMSTRINGS, vm.state.value.filter)
        assertEquals(
            listOf("BM_SIT_AND_REACH"),
            vm.state.value.visibleCards.map { it.benchmark.id },
        )
    }

    @Test
    fun `onFilterChanged null restores All view`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        vm.onFilterChanged(Category.NECK)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.visibleCards.size)
        vm.onFilterChanged(null)
        advanceUntilIdle()
        assertEquals(3, vm.state.value.visibleCards.size)
        assertNull(vm.state.value.filter)
    }

    @Test
    fun `live log emission updates the affected card without restart`() = runTest(testDispatcher) {
        val logsFlow = MutableStateFlow<List<BenchmarkLog>>(emptyList())
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor("BM_CERVICAL_ROTATION") } returns logsFlow
        coEvery { repo.observeLogsFor("BM_KNEE_TO_WALL") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_SIT_AND_REACH") } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        assertNull(vm.state.value.cards.first { it.benchmark.id == "BM_CERVICAL_ROTATION" }.latestRawValue)

        logsFlow.value = listOf(log("BM_CERVICAL_ROTATION", "70", midday(2026, 4, 23)))
        advanceUntilIdle()
        assertEquals(
            "70",
            vm.state.value.cards.first { it.benchmark.id == "BM_CERVICAL_ROTATION" }.latestRawValue,
        )
    }
}
