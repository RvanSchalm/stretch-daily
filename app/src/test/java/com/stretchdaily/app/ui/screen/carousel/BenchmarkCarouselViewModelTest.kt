package com.stretchdaily.app.ui.screen.carousel

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.data.BenchmarkRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalCoroutinesApi::class)
class BenchmarkCarouselViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: BenchmarkRepository

    private fun bm(id: String, type: BenchmarkInputType = BenchmarkInputType.NUMERIC) =
        Benchmark(
            id = id,
            name = id,
            category = Category.SPINE,
            description = "",
            unit = "cm",
            inputType = type,
            tierRanges = emptyMap(),
        )

    private val ten = (1..10).map { bm("BM_$it") }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun viewModel(): BenchmarkCarouselViewModel =
        BenchmarkCarouselViewModel(repo)

    @Test
    fun `init loads all benchmarks`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(10, state.totalSteps)
        assertEquals(0, state.currentIndex)
        assertEquals("BM_1", state.currentBenchmark?.id)
    }

    @Test
    fun `setStep moves currentIndex`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        val vm = viewModel()
        advanceUntilIdle()

        vm.setStep(4)
        assertEquals(4, vm.state.value.currentIndex)
        assertEquals("BM_5", vm.state.value.currentBenchmark?.id)
    }

    @Test
    fun `setStep clamps negative or past-end values`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        val vm = viewModel()
        advanceUntilIdle()

        vm.setStep(-3)
        assertEquals(0, vm.state.value.currentIndex)
        vm.setStep(99)
        assertEquals(9, vm.state.value.currentIndex)
    }

    @Test
    fun `onSaveEntry numeric advances step`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        coEvery { repo.logNumeric(any(), any()) } returns Result.success(1L)
        val vm = viewModel()
        advanceUntilIdle()

        val captured = mutableListOf<CarouselEvent>()
        val job = launch { vm.events.toList(captured) }

        vm.onSaveEntry("5.5", tier = null)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.logNumeric("BM_1", "5.5") }
        assertEquals(listOf(CarouselEvent.Advance(1)), captured)
        job.cancel()
    }

    @Test
    fun `onSaveEntry categorical advances step and calls logCategorical`() = runTest(testDispatcher) {
        val carousel = listOf(bm("CAT_1", BenchmarkInputType.CATEGORICAL)) + ten
        coEvery { repo.observeAllBenchmarks() } returns flowOf(carousel)
        coEvery { repo.logCategorical(any(), any()) } returns 7L
        val vm = viewModel()
        advanceUntilIdle()

        val captured = mutableListOf<CarouselEvent>()
        val job = launch { vm.events.toList(captured) }

        vm.onSaveEntry("FLEXIBLE", tier = FlexibilityTier.FLEXIBLE)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.logCategorical("CAT_1", FlexibilityTier.FLEXIBLE) }
        assertEquals(listOf(CarouselEvent.Advance(1)), captured)
        job.cancel()
    }

    @Test
    fun `onSaveEntry on last step fires Finished`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        coEvery { repo.logNumeric(any(), any()) } returns Result.success(1L)
        val vm = viewModel()
        advanceUntilIdle()
        vm.setStep(9)

        val captured = mutableListOf<CarouselEvent>()
        val job = launch { vm.events.toList(captured) }

        vm.onSaveEntry("1.0", tier = null)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.logNumeric("BM_10", "1.0") }
        assertEquals(listOf(CarouselEvent.Finished), captured)
        job.cancel()
    }

    @Test
    fun `onSaveEntry parse failure sets errorMessage and does NOT advance`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        coEvery { repo.logNumeric(any(), any()) } returns
            Result.failure(IllegalArgumentException("Not a number: oops"))
        val vm = viewModel()
        advanceUntilIdle()

        val captured = mutableListOf<CarouselEvent>()
        val job = launch { vm.events.toList(captured) }

        vm.onSaveEntry("oops", tier = null)
        advanceUntilIdle()

        assertEquals("Not a number: oops", vm.state.value.errorMessage)
        assertEquals(0, vm.state.value.currentIndex)
        assertTrue(captured.isEmpty())
        job.cancel()
    }

    @Test
    fun `onClose emits Finished`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        val vm = viewModel()
        advanceUntilIdle()

        val captured = mutableListOf<CarouselEvent>()
        val job = launch { vm.events.toList(captured) }

        vm.onClose()
        advanceUntilIdle()

        assertEquals(listOf(CarouselEvent.Finished), captured)
        job.cancel()
    }

    @Test
    fun `dismissError clears errorMessage without changing index`() = runTest(testDispatcher) {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        coEvery { repo.logNumeric(any(), any()) } returns
            Result.failure(IllegalArgumentException("Not a number: oops"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onSaveEntry("oops", tier = null)
        advanceUntilIdle()
        assertEquals("Not a number: oops", vm.state.value.errorMessage)

        vm.dismissError()
        assertEquals(null, vm.state.value.errorMessage)
        assertEquals(0, vm.state.value.currentIndex)
    }
}
