package com.stretchdaily.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.stretchdaily.app.core.model.BenchmarkLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkLogDao {

    @Query("SELECT * FROM benchmark_logs ORDER BY loggedAt DESC")
    fun observeAll(): Flow<List<BenchmarkLog>>

    @Query("SELECT * FROM benchmark_logs WHERE benchmarkId = :benchmarkId ORDER BY loggedAt DESC")
    fun observeForBenchmark(benchmarkId: String): Flow<List<BenchmarkLog>>

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

    @Query("SELECT MAX(loggedAt) FROM benchmark_logs")
    suspend fun getLastLoggedAt(): Long?

    @Insert
    suspend fun insert(log: BenchmarkLog): Long

    @Query("DELETE FROM benchmark_logs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
