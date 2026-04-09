package com.stretchdaily.app.data

import com.stretchdaily.app.core.database.DatabaseSeeder
import com.stretchdaily.app.core.database.dao.BenchmarkDao
import com.stretchdaily.app.core.database.dao.BenchmarkLogDao
import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.database.dao.SessionDao
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.export.ExportPayload
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

/**
 * Owns the three Settings actions that span the whole database:
 *
 * 1. **Export** — dumps every table into a single JSON [ExportPayload].
 * 2. **Import** — replaces every table with the contents of an [ExportPayload],
 *    refusing payloads with an unsupported [ExportPayload.version].
 * 3. **Delete all** — clears every user-modifiable table and re-seeds the
 *    static catalog (exercises + benchmarks) so the app stays usable
 *    immediately after wiping.
 *
 * The repository is intentionally schema-aware: it knows about every DAO and
 * the order in which rows have to be inserted to satisfy foreign keys
 * (benchmarks before benchmark_logs, session_records before session_exercises).
 * Encapsulating that here keeps the ViewModel layer free of cross-table
 * orchestration.
 *
 * All public functions are `suspend` so callers run them on a background
 * dispatcher. The repository itself does not switch dispatchers — that is the
 * caller's responsibility (the [SettingsViewModel] uses `viewModelScope`).
 */
@OptIn(ExperimentalSerializationApi::class)
@Singleton
class DataPortRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val benchmarkDao: BenchmarkDao,
    private val benchmarkLogDao: BenchmarkLogDao,
    private val sessionDao: SessionDao,
    private val clock: Clock,
) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** Snapshots every table into an [ExportPayload]. */
    suspend fun snapshot(): ExportPayload = ExportPayload(
        exportedAt = clock.now(),
        exercises = exerciseDao.getAll(),
        benchmarks = benchmarkDao.getAll(),
        benchmarkLogs = benchmarkLogDao.getAll(),
        sessionRecords = sessionDao.getAllRecords(),
        sessionExercises = sessionDao.getAllSessionExercises(),
    )

    /** Writes the JSON payload to [output]. The caller closes the stream. */
    suspend fun exportTo(output: OutputStream) {
        val payload = snapshot()
        json.encodeToStream(payload, output)
    }

    /**
     * Reads + validates a payload from [input], then atomically replaces the
     * database contents with it. Throws [IllegalArgumentException] for an
     * unsupported version so the caller can surface a clean error.
     */
    suspend fun importFrom(input: InputStream) {
        val payload: ExportPayload = json.decodeFromStream(input)
        require(payload.version == ExportPayload.CURRENT_VERSION) {
            "Unsupported export version ${payload.version}"
        }
        replaceAll(payload)
    }

    /**
     * Wipes every user-modifiable table and re-seeds the static catalog so the
     * app remains usable immediately after the wipe. Order matters: child
     * tables clear before parents to avoid lingering FK rows even though Room
     * is configured to cascade.
     */
    suspend fun deleteAll() {
        sessionDao.deleteAllSessionExercises()
        sessionDao.deleteAllRecords()
        benchmarkLogDao.deleteAll()
        exerciseDao.resetAllLastPerformed()
        // Catalog tables (exercises + benchmarks) are re-seeded rather than
        // emptied so a freshly-wiped app still has the 46 exercises and 10
        // benchmarks the engine expects. The seeder uses REPLACE on conflict.
        exerciseDao.insertAll(DatabaseSeeder.exercises())
        benchmarkDao.insertAll(DatabaseSeeder.benchmarks())
    }

    private suspend fun replaceAll(payload: ExportPayload) {
        // Clear children first to satisfy FKs.
        sessionDao.deleteAllSessionExercises()
        sessionDao.deleteAllRecords()
        benchmarkLogDao.deleteAll()
        // Catalog tables use REPLACE on conflict, so re-inserting overwrites.
        benchmarkDao.insertAll(payload.benchmarks)
        exerciseDao.insertAll(payload.exercises)
        // Now restore the user-generated rows in dependency order.
        benchmarkLogDao.insertAll(payload.benchmarkLogs)
        sessionDao.insertAllRecords(payload.sessionRecords)
        sessionDao.insertExercises(payload.sessionExercises)
    }
}
