package com.stretchdaily.app.ui.session

import com.stretchdaily.app.core.model.SessionRecord
import com.stretchdaily.app.data.SessionRepository
import io.mockk.every
import io.mockk.mockk
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
class SessionHistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: SessionRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun record(id: Long, completedAt: Long): SessionRecord = SessionRecord(
        id = id,
        startedAt = completedAt - 600_000,
        completedAt = completedAt,
        totalDurationSeconds = 600,
        exerciseCount = 6,
    )

    @Test
    fun `empty repository emits Empty`() = runTest {
        every { repository.observeAllSessions() } returns MutableStateFlow(emptyList())

        val vm = SessionHistoryViewModel(repository)
        advanceUntilIdle()

        assertEquals(SessionHistoryUiState.Empty, vm.state.value)
    }

    @Test
    fun `non empty repository emits Loaded with the records`() = runTest {
        val records = listOf(
            record(id = 1, completedAt = 1_700_000_000_000L),
            record(id = 2, completedAt = 1_700_100_000_000L),
        )
        every { repository.observeAllSessions() } returns MutableStateFlow(records)

        val vm = SessionHistoryViewModel(repository)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("expected Loaded, was $state", state is SessionHistoryUiState.Loaded)
        assertEquals(records, (state as SessionHistoryUiState.Loaded).records)
    }
}
