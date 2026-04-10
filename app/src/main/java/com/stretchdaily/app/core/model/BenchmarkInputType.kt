package com.stretchdaily.app.core.model

import kotlinx.serialization.Serializable

/**
 * How the user enters their result for a benchmark.
 *
 * - [NUMERIC]: user types a value (cm or degrees) and the app resolves the
 *   tier from the benchmark's stored thresholds.
 * - [CATEGORICAL]: user picks the tier directly from qualitative descriptions
 *   (currently only the ATG Split Squat).
 */
@Serializable
enum class BenchmarkInputType {
    NUMERIC,
    CATEGORICAL
}
