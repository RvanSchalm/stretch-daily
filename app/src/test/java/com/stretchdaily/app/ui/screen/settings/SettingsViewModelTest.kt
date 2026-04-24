package com.stretchdaily.app.ui.screen.settings

import android.content.Context
import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.datastore.SettingsDataStore
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.data.BenchmarkRepository
import com.stretchdaily.app.data.DataPortRepository
import com.stretchdaily.app.data.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var appContext: Context
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var dataPortRepository: DataPortRepository
    private lateinit var benchmarkRepository: BenchmarkRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var testDispatcher: UnconfinedTestDispatcher

    private val audioFlow = MutableStateFlow(true)
    private val bannerFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        appContext = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)
        dataPortRepository = mockk(relaxed = true)
        benchmarkRepository = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)

        every { settingsDataStore.audioCuesEnabled } returns audioFlow
        every { settingsDataStore.benchmarkBannerEnabled } returns bannerFlow
        every { exerciseDao.observeAll() } returns flowOf(exercises(46, categories = 7))
        every { benchmarkRepository.observeAllBenchmarks() } returns flowOf(benchmarks(10))
        every { sessionRepository.totalsFlow } returns flowOf(SessionRepository.Totals(sessions = 12, totalMinutes = 120))
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm(): SettingsViewModel = SettingsViewModel(
        appContext = appContext,
        settingsDataStore = settingsDataStore,
        dataPortRepository = dataPortRepository,
        benchmarkRepository = benchmarkRepository,
        sessionRepository = sessionRepository,
        exerciseDao = exerciseDao,
    )

    @Test
    fun `initial state reflects flows`() = runTest(testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val s = vm.state.value
        assertTrue(s.audioCuesEnabled)
        assertFalse(s.benchmarkBannerEnabled)
        assertEquals(46, s.libraryStats.exerciseCount)
        assertEquals(7, s.libraryStats.categoryCount)
        assertEquals(10, s.libraryStats.benchmarkCount)
        assertEquals(12, s.libraryStats.sessionsLogged)
        assertEquals(SettingsStatus.Idle, s.status)
    }

    @Test
    fun `setAudioCuesEnabled delegates to DataStore`() = runTest(testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.setAudioCuesEnabled(false)
        advanceUntilIdle()
        coVerify { settingsDataStore.setAudioCuesEnabled(false) }
    }

    @Test
    fun `setBenchmarkBannerEnabled delegates to DataStore`() = runTest(testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.setBenchmarkBannerEnabled(true)
        advanceUntilIdle()
        coVerify { settingsDataStore.setBenchmarkBannerEnabled(true) }
    }

    @Test
    fun `banner flow emission updates state`() = runTest(testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        bannerFlow.value = true
        advanceUntilIdle()
        assertTrue(vm.state.value.benchmarkBannerEnabled)
    }

    @Test
    fun `categoryCount dedupes by category`() = runTest(testDispatcher) {
        every { exerciseDao.observeAll() } returns flowOf(
            exercises(count = 6, categories = 2),
        )
        val vm = vm()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.libraryStats.categoryCount)
    }

    @Test
    fun `showDeleteDialog toggles state`() = runTest(testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertFalse(vm.state.value.showDeleteDialog)
        vm.showDeleteDialog()
        advanceUntilIdle()
        assertTrue(vm.state.value.showDeleteDialog)
        vm.dismissDeleteDialog()
        advanceUntilIdle()
        assertFalse(vm.state.value.showDeleteDialog)
    }

    @Test
    fun `deleteAllData dismisses dialog and runs deletion`() = runTest(testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.showDeleteDialog()
        advanceUntilIdle()
        vm.deleteAllData()
        advanceUntilIdle()
        assertFalse(vm.state.value.showDeleteDialog)
        coVerify { dataPortRepository.deleteAll() }
        assertTrue(vm.state.value.status is SettingsStatus.Success)
    }

    @Test
    fun `consumeStatus resets to Idle`() = runTest(testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.deleteAllData()
        advanceUntilIdle()
        assertTrue(vm.state.value.status is SettingsStatus.Success)
        vm.consumeStatus()
        advanceUntilIdle()
        assertEquals(SettingsStatus.Idle, vm.state.value.status)
    }

    private fun exercises(count: Int, categories: Int): List<Exercise> {
        val cats = Category.values().take(categories)
        return List(count) { i ->
            Exercise(
                id = "EX$i",
                name = "Exercise $i",
                category = cats[i % categories],
                cues = emptyList(),
                isUnilateral = false,
                isTimed = true,
                targetReps = null,
                secondsPerRep = null,
                totalTime = 30,
                lastPerformed = null,
            )
        }
    }

    private fun benchmarks(count: Int): List<Benchmark> = List(count) { i ->
        Benchmark(
            id = "BM$i",
            name = "B $i",
            category = Category.NECK,
            description = "",
            unit = "°",
            inputType = BenchmarkInputType.NUMERIC,
            tierRanges = emptyMap(),
        )
    }
}
