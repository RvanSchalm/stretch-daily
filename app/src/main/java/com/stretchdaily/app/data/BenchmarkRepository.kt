package com.stretchdaily.app.data

import com.stretchdaily.app.core.benchmark.TierResolver
import com.stretchdaily.app.core.database.dao.BenchmarkDao
import com.stretchdaily.app.core.database.dao.BenchmarkLogDao
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.core.util.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Repository for benchmarks and their logs. Wraps the two DAOs plus the
 * [TierResolver] so the UI layer never has to touch Room or the resolver
 * directly. Purely-logical helpers (`isBenchmarksDue`) live as `internal`
 * companion functions so they can be unit tested without Room — same
 * pattern as [SessionRepository.computeStreak].
 */
@Singleton
class BenchmarkRepository @Inject constructor(
    private val benchmarkDao: BenchmarkDao,
    private val benchmarkLogDao: BenchmarkLogDao,
    private val tierResolver: TierResolver,
    private val clock: Clock,
) {

    suspend fun getAllBenchmarks(): List<Benchmark> = benchmarkDao.getAll()

    suspend fun getBenchmark(id: String): Benchmark? = benchmarkDao.getById(id)

    suspend fun getLatestLogs(): List<BenchmarkLog> =
        benchmarkLogDao.getLatestPerBenchmark()

    /**
     * Reactive views of the catalog and the latest log per benchmark. The
     * Benchmarks tab subscribes to both so it stays in sync with database
     * mutations performed elsewhere — including the Settings "Delete all
     * data" action and the future Import flow.
     */
    fun observeAllBenchmarks(): Flow<List<Benchmark>> = benchmarkDao.observeAll()

    fun observeLatestLogs(): Flow<List<BenchmarkLog>> =
        benchmarkLogDao.observeLatestPerBenchmark()

    fun observeLogsFor(benchmarkId: String): Flow<List<BenchmarkLog>> =
        benchmarkLogDao.observeForBenchmark(benchmarkId)

    suspend fun getLog(id: Long): BenchmarkLog? = benchmarkLogDao.getById(id)

    /**
     * Logs a numeric benchmark reading. The raw value is kept verbatim for
     * display; the resolved tier is what the engine consumes.
     */
    suspend fun logNumeric(benchmarkId: String, rawValue: String): Result<Long> =
        runCatching {
            val value = parseNumeric(rawValue)
            val tier = tierResolver.resolve(benchmarkId, value)
                ?: error("No tier profile for $benchmarkId")
            benchmarkLogDao.insert(
                BenchmarkLog(
                    benchmarkId = benchmarkId,
                    rawValue = rawValue,
                    resolvedTier = tier,
                    loggedAt = clock.now(),
                )
            )
        }

    /**
     * Logs the single categorical benchmark (ATG Split Squat). The tier is
     * chosen directly by the user; there's no parsing involved.
     */
    suspend fun logCategorical(benchmarkId: String, tier: FlexibilityTier): Long =
        benchmarkLogDao.insert(
            BenchmarkLog(
                benchmarkId = benchmarkId,
                rawValue = tier.name,
                resolvedTier = tier,
                loggedAt = clock.now(),
            )
        )

    /**
     * Updates an existing log entry. For numeric benchmarks we re-resolve the
     * tier from the new raw value; for categorical we trust [tierOverride].
     */
    suspend fun updateLog(
        existing: BenchmarkLog,
        rawValue: String,
        tierOverride: FlexibilityTier? = null,
        benchmarkInputType: BenchmarkInputType,
    ): Result<Unit> = runCatching {
        val tier = when (benchmarkInputType) {
            BenchmarkInputType.NUMERIC -> {
                val value = parseNumeric(rawValue)
                tierResolver.resolve(existing.benchmarkId, value)
                    ?: error("No tier profile for ${existing.benchmarkId}")
            }
            BenchmarkInputType.CATEGORICAL -> tierOverride
                ?: error("Categorical update requires a tier")
        }
        benchmarkLogDao.update(
            existing.copy(rawValue = rawValue, resolvedTier = tier)
        )
    }

    suspend fun deleteLog(id: Long) = benchmarkLogDao.deleteById(id)

    /**
     * True if the Home banner should nag the user to re-log. Either the user
     * has never logged, or their most recent log is older than 30 days, or
     * it's the 1st of the month and they haven't logged yet today.
     */
    suspend fun isBenchmarksDue(zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
        val lastLoggedAt = benchmarkLogDao.getLastLoggedAt()
        return computeBenchmarksDue(lastLoggedAt, clock.now(), zoneId)
    }

    private fun parseNumeric(raw: String): Double {
        val normalised = raw.trim().replace(',', '.')
        return normalised.toDoubleOrNull()
            ?: throw IllegalArgumentException("Not a number: $raw")
    }

    companion object {
        internal const val STALE_AFTER_DAYS: Long = 30

        /**
         * Pure-Kotlin nagging logic. Extracted from [isBenchmarksDue] so it can
         * be tested without Room.
         */
        internal fun computeBenchmarksDue(
            lastLoggedAt: Long?,
            now: Long,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): Boolean {
            if (lastLoggedAt == null) return true
            val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zoneId)
            val lastDate = LocalDate.ofInstant(Instant.ofEpochMilli(lastLoggedAt), zoneId)
            val daysSince = java.time.temporal.ChronoUnit.DAYS.between(lastDate, today)
            if (daysSince > STALE_AFTER_DAYS) return true
            // 1st-of-month nudge: if today is the 1st and the user hasn't
            // logged today yet, remind them to recheck their baselines.
            if (today.dayOfMonth == 1 && lastDate != today) return true
            return false
        }
    }
}
