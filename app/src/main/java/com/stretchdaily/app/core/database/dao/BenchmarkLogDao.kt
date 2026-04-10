package com.stretchdaily.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.stretchdaily.app.core.model.BenchmarkLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkLogDao {

    @Query("SELECT * FROM benchmark_logs ORDER BY loggedAt DESC")
    fun observeAll(): Flow<List<BenchmarkLog>>

    @Query("SELECT * FROM benchmark_logs WHERE benchmarkId = :benchmarkId ORDER BY loggedAt DESC")
    fun observeForBenchmark(benchmarkId: String): Flow<List<BenchmarkLog>>

    @Query("SELECT * FROM benchmark_logs WHERE id = :id")
    suspend fun getById(id: Long): BenchmarkLog?

    /**
     * Latest log per benchmark — what the [CategoryWeightCalculator] consumes
     * to derive the current tier for each category.
     */
    @Query(
        """
        SELECT * FROM benchmark_logs
        WHERE id IN (
            SELECT MAX(id) FROM benchmark_logs GROUP BY benchmarkId
        )
        """
    )
    suspend fun getLatestPerBenchmark(): List<BenchmarkLog>

    /**
     * Reactive version of [getLatestPerBenchmark]. Used by the Benchmarks tab
     * so the latest-value badges update immediately when logs are inserted,
     * edited, deleted, or wiped wholesale by the Settings "Delete all data"
     * action.
     */
    @Query(
        """
        SELECT * FROM benchmark_logs
        WHERE id IN (
            SELECT MAX(id) FROM benchmark_logs GROUP BY benchmarkId
        )
        """
    )
    fun observeLatestPerBenchmark(): Flow<List<BenchmarkLog>>

    @Query("SELECT MAX(loggedAt) FROM benchmark_logs")
    suspend fun getLastLoggedAt(): Long?

    @Insert
    suspend fun insert(log: BenchmarkLog): Long

    @Insert
    suspend fun insertAll(logs: List<BenchmarkLog>)

    @Update
    suspend fun update(log: BenchmarkLog)

    @Query("DELETE FROM benchmark_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM benchmark_logs")
    suspend fun deleteAll()

    /** Read-side helper used by the data exporter. */
    @Query("SELECT * FROM benchmark_logs ORDER BY id")
    suspend fun getAll(): List<BenchmarkLog>
}
