package com.stretchdaily.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Header row for a completed session. Powers the dashboard streak / volume
 * KPIs and the per-exercise history join via [SessionExercise].
 */
@Entity(tableName = "session_records")
@Serializable
data class SessionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val completedAt: Long,
    val totalDurationSeconds: Int,
    val exerciseCount: Int
)
