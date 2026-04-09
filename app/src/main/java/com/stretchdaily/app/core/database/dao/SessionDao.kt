package com.stretchdaily.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.stretchdaily.app.core.model.SessionExercise
import com.stretchdaily.app.core.model.SessionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM session_records ORDER BY completedAt DESC")
    fun observeAllRecords(): Flow<List<SessionRecord>>

    @Query("SELECT COUNT(*) FROM session_records")
    suspend fun getTotalCount(): Int

    @Query("SELECT MAX(completedAt) FROM session_records")
    suspend fun getLastCompletedAt(): Long?

    @Query("SELECT completedAt FROM session_records ORDER BY completedAt DESC")
    suspend fun getAllCompletionTimestamps(): List<Long>

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getExercisesForSession(sessionId: Long): List<SessionExercise>

    @Insert
    suspend fun insertRecord(record: SessionRecord): Long

    @Insert
    suspend fun insertExercises(exercises: List<SessionExercise>)

    /** Read-side helpers used by the data exporter. */
    @Query("SELECT * FROM session_records ORDER BY id")
    suspend fun getAllRecords(): List<SessionRecord>

    @Query("SELECT * FROM session_exercises ORDER BY id")
    suspend fun getAllSessionExercises(): List<SessionExercise>

    /** Bulk-insert helpers used by the data importer. */
    @Insert
    suspend fun insertAllRecords(records: List<SessionRecord>)

    /**
     * Cascade-deletes session_exercises via the FK on session_records.
     * "Delete all data" calls this for both tables.
     */
    @Query("DELETE FROM session_records")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM session_exercises")
    suspend fun deleteAllSessionExercises()
}
