package com.stretchdaily.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A measurable mobility test (e.g. Sit and Reach, Knee-to-Wall) used to assess
 * one [category]. There are 10 of these in total — 9 numeric, 1 categorical.
 *
 * For numeric benchmarks, [tierRanges] holds the textual range definitions
 * keyed by tier (e.g. "STIFF" -> "< -10 cm"). The TierResolver parses these at
 * runtime to map a raw value to a [FlexibilityTier].
 *
 * For categorical benchmarks, [tierRanges] instead holds the qualitative
 * description shown next to each tier button.
 */
@Entity(tableName = "benchmarks")
data class Benchmark(
    @PrimaryKey val id: String,
    val name: String,
    val category: Category,
    val description: String,
    val unit: String,
    val inputType: BenchmarkInputType,
    val tierRanges: Map<String, String>
)
