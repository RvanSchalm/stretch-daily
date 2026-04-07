package com.stretchdaily.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stretchdaily.app.core.model.Benchmark
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {

    @Query("SELECT * FROM benchmarks ORDER BY category, name")
    fun observeAll(): Flow<List<Benchmark>>

    @Query("SELECT * FROM benchmarks")
    suspend fun getAll(): List<Benchmark>

    @Query("SELECT * FROM benchmarks WHERE id = :id")
    suspend fun getById(id: String): Benchmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(benchmarks: List<Benchmark>)
}
