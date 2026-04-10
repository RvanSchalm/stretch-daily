package com.stretchdaily.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A single mobility / tendon exercise that the engine may schedule into a
 * session.
 *
 * Notes on the time fields:
 * - [totalTime] is what the engine budgets against. For unilateral exercises
 *   it already accounts for both sides (i.e. it is the on-screen total).
 * - [secondsPerRep] is only meaningful for rep-based exercises. The
 *   follow-along screen multiplies it by [targetReps] when no fixed timer
 *   applies.
 * - [lastPerformed] is updated when a session completes. The Selection Shield
 *   uses it to force exercises that have been idle for >14 days back into
 *   rotation.
 */
@Entity(tableName = "exercises")
@Serializable
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val category: Category,
    val cues: List<String>,
    val isUnilateral: Boolean,
    val isTimed: Boolean,
    val targetReps: Int?,
    val secondsPerRep: Int?,
    val totalTime: Int,
    val lastPerformed: Long? = null
)
