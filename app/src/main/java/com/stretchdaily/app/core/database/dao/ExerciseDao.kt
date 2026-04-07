package com.stretchdaily.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises")
    suspend fun getAll(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE category = :category")
    suspend fun getByCategory(category: Category): List<Exercise>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): Exercise?

    /**
     * Returns exercises that have either never been performed or whose last
     * session is older than [cutoffEpochMillis]. The Selection Shield uses
     * this list to force stale exercises into the next session.
     */
    @Query("SELECT * FROM exercises WHERE lastPerformed IS NULL OR lastPerformed < :cutoffEpochMillis")
    suspend fun getStale(cutoffEpochMillis: Long): List<Exercise>

    @Query("UPDATE exercises SET lastPerformed = :timestamp WHERE id = :id")
    suspend fun updateLastPerformed(id: String, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)
}
