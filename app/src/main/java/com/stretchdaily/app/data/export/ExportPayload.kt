package com.stretchdaily.app.data.export

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.model.SessionExercise
import com.stretchdaily.app.core.model.SessionRecord
import kotlinx.serialization.Serializable

/**
 * Versioned wrapper for the JSON export/import file.
 *
 * The schema is intentionally a flat dump of the Room tables — same field
 * names, same types — so a future restore can re-insert the rows verbatim.
 * The version field exists so we can detect (and refuse) older or future
 * payloads should the schema ever change.
 *
 * Why all five tables: benchmarks and exercises are otherwise re-seeded from
 * code and would have the same content on a fresh install, but we still want
 * to preserve any per-row state (e.g. `Exercise.lastPerformed`) on import.
 * Round-tripping the catalog is the cheapest way to keep that state coherent.
 */
@Serializable
data class ExportPayload(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val exercises: List<Exercise>,
    val benchmarks: List<Benchmark>,
    val benchmarkLogs: List<BenchmarkLog>,
    val sessionRecords: List<SessionRecord>,
    val sessionExercises: List<SessionExercise>,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
