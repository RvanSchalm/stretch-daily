package com.stretchdaily.app.core.benchmark

import com.stretchdaily.app.core.model.FlexibilityTier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a raw numeric benchmark reading to a [FlexibilityTier].
 *
 * Each numeric benchmark has a fixed set of four breakpoints separating the
 * five tiers. Five of the benchmarks are "ascending" — higher numbers mean
 * more flexibility (e.g. Cervical Rotation degrees, Knee-to-Wall distance).
 * The other four are "descending" — lower numbers mean more flexibility
 * (Apley Scratch gap, Butterfly knee-to-floor, Sit and Reach — which even
 * goes negative — and Thomas Test, where "up" angles are stiff hip flexors
 * and "down" angles are flexible ones).
 *
 * The blueprint briefly considered parsing [com.stretchdaily.app.core.model.Benchmark.tierRanges]
 * at runtime, but those strings are free-form display copy ("> 15° up",
 * "< 0 cm (Finger Overlap)") — parsing them cleanly would be more fragile
 * than just hard-coding the ten numeric profiles next to the seed data they
 * describe. The sheet-facing strings still live in `tierRanges` for UI
 * display; this file is the single source of truth for the numeric math.
 */
@Singleton
class TierResolver @Inject constructor() {

    /**
     * Resolves [value] for the benchmark identified by [benchmarkId]. Returns
     * `null` for unknown IDs and for the single categorical benchmark
     * (`BM_ATG_SPLIT_SQUAT`) — the caller picks the tier directly in that
     * case, there's nothing to resolve.
     */
    fun resolve(benchmarkId: String, value: Double): FlexibilityTier? =
        PROFILES[benchmarkId]?.resolve(value)

    /** Whether a logged value was for a benchmark this resolver handles. */
    fun handles(benchmarkId: String): Boolean = benchmarkId in PROFILES

    private enum class Direction { ASCENDING, DESCENDING }

    /**
     * Five-tier profile. [breakpoints] are ordered stiffest-to-most-flexible.
     * For [Direction.ASCENDING] a value strictly below `breakpoints[i]` lands
     * in tier `i`; for [Direction.DESCENDING] a value strictly above
     * `breakpoints[i]` lands in tier `i`. Anything past the last breakpoint
     * is [FlexibilityTier.VERY_FLEXIBLE].
     */
    private data class Profile(
        val direction: Direction,
        val breakpoints: List<Double>,
    ) {
        fun resolve(value: Double): FlexibilityTier {
            breakpoints.forEachIndexed { i, bp ->
                val stillStiffer = when (direction) {
                    Direction.ASCENDING -> value < bp
                    Direction.DESCENDING -> value > bp
                }
                if (stillStiffer) return TIERS[i]
            }
            return FlexibilityTier.VERY_FLEXIBLE
        }
    }

    companion object {
        private val TIERS = listOf(
            FlexibilityTier.STIFF,
            FlexibilityTier.BELOW_AVERAGE,
            FlexibilityTier.AVERAGE,
            FlexibilityTier.FLEXIBLE,
            FlexibilityTier.VERY_FLEXIBLE,
        )

        // Keep these IDs aligned with DatabaseSeeder.benchmarks().
        private val PROFILES: Map<String, Profile> = mapOf(
            "BM_CERVICAL_ROTATION" to Profile(Direction.ASCENDING, listOf(50.0, 70.0, 80.0, 90.0)),
            "BM_THORACIC_ROTATION" to Profile(Direction.ASCENDING, listOf(25.0, 35.0, 45.0, 55.0)),
            "BM_KNEE_TO_WALL" to Profile(Direction.ASCENDING, listOf(5.0, 8.0, 10.0, 13.0)),
            "BM_WRIST_EXTENSION" to Profile(Direction.ASCENDING, listOf(60.0, 70.0, 80.0, 90.0)),
            "BM_WRIST_FLEXION" to Profile(Direction.ASCENDING, listOf(65.0, 75.0, 85.0, 95.0)),
            "BM_APLEY_SCRATCH" to Profile(Direction.DESCENDING, listOf(15.0, 10.0, 5.0, 0.0)),
            "BM_BUTTERFLY" to Profile(Direction.DESCENDING, listOf(20.0, 15.0, 10.0, 5.0)),
            // Sit and Reach: positive = short of toes, negative = past toes.
            // More negative is more flexible.
            "BM_SIT_AND_REACH" to Profile(Direction.DESCENDING, listOf(20.0, 10.0, 0.0, -10.0)),
            // Thomas Test: positive angle = thigh above horizontal (stiff hip
            // flexor); negative = thigh hangs below horizontal (flexible).
            "BM_THOMAS_TEST" to Profile(Direction.DESCENDING, listOf(15.0, 5.0, -5.0, -10.0)),
        )
    }
}
