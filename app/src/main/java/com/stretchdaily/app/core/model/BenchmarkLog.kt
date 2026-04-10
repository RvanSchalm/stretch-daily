package com.stretchdaily.app.core.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * One assessment result for a [Benchmark].
 *
 * [rawValue] keeps the original user input as a string so we can show the
 * exact value back later (e.g. "12.5" cm or the chosen tier name for a
 * categorical benchmark). [resolvedTier] is what the Longevity Engine reads
 * when computing category weights.
 */
@Entity(
    tableName = "benchmark_logs",
    foreignKeys = [
        ForeignKey(
            entity = Benchmark::class,
            parentColumns = ["id"],
            childColumns = ["benchmarkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("benchmarkId"), Index("loggedAt")]
)
@Serializable
data class BenchmarkLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val benchmarkId: String,
    val rawValue: String,
    val resolvedTier: FlexibilityTier,
    val loggedAt: Long
)
