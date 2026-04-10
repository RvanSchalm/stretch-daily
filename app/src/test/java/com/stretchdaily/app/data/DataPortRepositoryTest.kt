package com.stretchdaily.app.data

import com.stretchdaily.app.core.database.DatabaseSeeder
import com.stretchdaily.app.core.database.dao.BenchmarkDao
import com.stretchdaily.app.core.database.dao.BenchmarkLogDao
import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.database.dao.SessionDao
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.core.model.SessionExercise
import com.stretchdaily.app.core.model.SessionRecord
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.export.ExportPayload
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataPortRepositoryTest {

    private lateinit var exerciseDao: ExerciseDao
    private lateinit var benchmarkDao: BenchmarkDao
    private lateinit var benchmarkLogDao: BenchmarkLogDao
    private lateinit var sessionDao: SessionDao
    private val clock = Clock { FIXED_NOW }

    private lateinit var repository: DataPortRepository

    private val exerciseFixture = Exercise(
        id = "N01",
        name = "Chin Tucks",
        category = Category.NECK,
        cues = listOf("Sit tall", "Pull chin back", "Hold 5s"),
        isUnilateral = false,
        isTimed = false,
        targetReps = 10,
        secondsPerRep = 5,
        totalTime = 50,
        lastPerformed = 1_700_000_000_000L,
    )

    private val benchmarkFixture = Benchmark(
        id = "BM_KNEE_TO_WALL",
        name = "Knee-to-Wall",
        category = Category.ANKLES,
        description = "How far the knee passes the toes",
        unit = "cm",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = mapOf("STIFF" to "< 5 cm", "AVERAGE" to "5-10 cm"),
    )

    private val benchmarkLogFixture = BenchmarkLog(
        id = 7,
        benchmarkId = "BM_KNEE_TO_WALL",
        rawValue = "8",
        resolvedTier = FlexibilityTier.AVERAGE,
        loggedAt = 1_700_100_000_000L,
    )

    private val sessionRecordFixture = SessionRecord(
        id = 3,
        startedAt = 1_700_200_000_000L,
        completedAt = 1_700_200_900_000L,
        totalDurationSeconds = 900,
        exerciseCount = 6,
    )

    private val sessionExerciseFixture = SessionExercise(
        id = 11,
        sessionId = 3,
        exerciseId = "N01",
        orderIndex = 0,
        durationSeconds = 50,
    )

    @Before
    fun setup() {
        exerciseDao = mockk(relaxed = true)
        benchmarkDao = mockk(relaxed = true)
        benchmarkLogDao = mockk(relaxed = true)
        sessionDao = mockk(relaxed = true)
        repository = DataPortRepository(
            exerciseDao = exerciseDao,
            benchmarkDao = benchmarkDao,
            benchmarkLogDao = benchmarkLogDao,
            sessionDao = sessionDao,
            clock = clock,
        )
    }

    @Test
    fun `snapshot pulls from every dao and stamps the export time`() = runTest {
        coEvery { exerciseDao.getAll() } returns listOf(exerciseFixture)
        coEvery { benchmarkDao.getAll() } returns listOf(benchmarkFixture)
        coEvery { benchmarkLogDao.getAll() } returns listOf(benchmarkLogFixture)
        coEvery { sessionDao.getAllRecords() } returns listOf(sessionRecordFixture)
        coEvery { sessionDao.getAllSessionExercises() } returns listOf(sessionExerciseFixture)

        val payload = repository.snapshot()

        assertEquals(ExportPayload.CURRENT_VERSION, payload.version)
        assertEquals(FIXED_NOW, payload.exportedAt)
        assertEquals(listOf(exerciseFixture), payload.exercises)
        assertEquals(listOf(benchmarkFixture), payload.benchmarks)
        assertEquals(listOf(benchmarkLogFixture), payload.benchmarkLogs)
        assertEquals(listOf(sessionRecordFixture), payload.sessionRecords)
        assertEquals(listOf(sessionExerciseFixture), payload.sessionExercises)
    }

    @Test
    fun `exportTo writes a JSON document the repository can re-import`() = runTest {
        coEvery { exerciseDao.getAll() } returns listOf(exerciseFixture)
        coEvery { benchmarkDao.getAll() } returns listOf(benchmarkFixture)
        coEvery { benchmarkLogDao.getAll() } returns listOf(benchmarkLogFixture)
        coEvery { sessionDao.getAllRecords() } returns listOf(sessionRecordFixture)
        coEvery { sessionDao.getAllSessionExercises() } returns listOf(sessionExerciseFixture)

        val buffer = ByteArrayOutputStream()
        repository.exportTo(buffer)

        val text = buffer.toString(Charsets.UTF_8)
        assertTrue("export should be non-empty JSON", text.isNotBlank())
        assertTrue("export should mention the seeded exercise id", text.contains("N01"))
        assertTrue(
            "export should mention the benchmark id",
            text.contains("BM_KNEE_TO_WALL"),
        )

        // Round-trip via importFrom — capture what each DAO is asked to insert
        // and assert it matches what we exported.
        val benchmarksSlot = slot<List<Benchmark>>()
        val exercisesSlot = slot<List<Exercise>>()
        val logsSlot = slot<List<BenchmarkLog>>()
        val recordsSlot = slot<List<SessionRecord>>()
        val sessionExercisesSlot = slot<List<SessionExercise>>()
        coEvery { benchmarkDao.insertAll(capture(benchmarksSlot)) } returns Unit
        coEvery { exerciseDao.insertAll(capture(exercisesSlot)) } returns Unit
        coEvery { benchmarkLogDao.insertAll(capture(logsSlot)) } returns Unit
        coEvery { sessionDao.insertAllRecords(capture(recordsSlot)) } returns Unit
        coEvery { sessionDao.insertExercises(capture(sessionExercisesSlot)) } returns Unit

        repository.importFrom(ByteArrayInputStream(buffer.toByteArray()))

        assertEquals(listOf(benchmarkFixture), benchmarksSlot.captured)
        assertEquals(listOf(exerciseFixture), exercisesSlot.captured)
        assertEquals(listOf(benchmarkLogFixture), logsSlot.captured)
        assertEquals(listOf(sessionRecordFixture), recordsSlot.captured)
        assertEquals(listOf(sessionExerciseFixture), sessionExercisesSlot.captured)
    }

    @Test
    fun `importFrom rejects an unsupported version`() = runTest {
        val futureJson = """
            {
              "version": 999,
              "exportedAt": 1,
              "exercises": [],
              "benchmarks": [],
              "benchmarkLogs": [],
              "sessionRecords": [],
              "sessionExercises": []
            }
        """.trimIndent()

        var caught: IllegalArgumentException? = null
        try {
            repository.importFrom(ByteArrayInputStream(futureJson.toByteArray()))
        } catch (e: IllegalArgumentException) {
            caught = e
        }
        assertNotNull("expected IllegalArgumentException for version mismatch", caught)
        assertTrue(
            "error should mention the offending version",
            caught!!.message!!.contains("999"),
        )
        // No DAO writes should happen when validation fails up front.
        coVerify(exactly = 0) { sessionDao.deleteAllRecords() }
        coVerify(exactly = 0) { benchmarkDao.insertAll(any()) }
    }

    @Test
    fun `importFrom clears children before parents and reinserts in FK order`() = runTest {
        // Round-trip a tiny payload through serialization so we exercise the
        // real Json instance, then assert the DAO call order.
        coEvery { exerciseDao.getAll() } returns listOf(exerciseFixture)
        coEvery { benchmarkDao.getAll() } returns listOf(benchmarkFixture)
        coEvery { benchmarkLogDao.getAll() } returns listOf(benchmarkLogFixture)
        coEvery { sessionDao.getAllRecords() } returns listOf(sessionRecordFixture)
        coEvery { sessionDao.getAllSessionExercises() } returns listOf(sessionExerciseFixture)
        val buffer = ByteArrayOutputStream()
        repository.exportTo(buffer)

        repository.importFrom(ByteArrayInputStream(buffer.toByteArray()))

        coVerifyOrder {
            // Children first, then parents.
            sessionDao.deleteAllSessionExercises()
            sessionDao.deleteAllRecords()
            benchmarkLogDao.deleteAll()
            // Catalog (REPLACE on conflict) — order between these two doesn't
            // matter for FKs but the implementation does benchmarks first.
            benchmarkDao.insertAll(any())
            exerciseDao.insertAll(any())
            // Then user data, parents before children.
            benchmarkLogDao.insertAll(any())
            sessionDao.insertAllRecords(any())
            sessionDao.insertExercises(any())
        }
    }

    @Test
    fun `deleteAll wipes user tables and reseeds the catalog`() = runTest {
        repository.deleteAll()

        coVerifyOrder {
            sessionDao.deleteAllSessionExercises()
            sessionDao.deleteAllRecords()
            benchmarkLogDao.deleteAll()
            exerciseDao.resetAllLastPerformed()
        }
        // Catalog re-seed: should hand the DAOs the full DatabaseSeeder lists
        // so the app stays usable immediately after a wipe.
        val exercisesSlot = slot<List<Exercise>>()
        val benchmarksSlot = slot<List<Benchmark>>()
        coVerify { exerciseDao.insertAll(capture(exercisesSlot)) }
        coVerify { benchmarkDao.insertAll(capture(benchmarksSlot)) }
        assertEquals(DatabaseSeeder.exercises().size, exercisesSlot.captured.size)
        assertEquals(DatabaseSeeder.benchmarks().size, benchmarksSlot.captured.size)
    }

    private companion object {
        const val FIXED_NOW = 1_700_500_000_000L
    }
}
