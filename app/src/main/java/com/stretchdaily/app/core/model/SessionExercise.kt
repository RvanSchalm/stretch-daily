package com.stretchdaily.app.core.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * One exercise inside a [SessionRecord]. Insertion order is preserved through
 * [orderIndex] so the completion screen and history can replay the session
 * exactly as the user did it.
 */
@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = SessionRecord::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
@Serializable
data class SessionExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val orderIndex: Int,
    val durationSeconds: Int
)
