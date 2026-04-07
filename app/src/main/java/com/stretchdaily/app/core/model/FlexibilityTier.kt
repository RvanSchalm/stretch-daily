package com.stretchdaily.app.core.model

/**
 * Five-level rating used by every benchmark.
 *
 * The [weight] is consumed by the Longevity Engine: stiffer tiers carry larger
 * weights so the weighted-random exercise selection picks from those categories
 * more often. Numbers are deliberately spaced to make weak areas dominate
 * without completely starving healthy ones.
 */
enum class FlexibilityTier(val displayName: String, val weight: Double) {
    STIFF("Stiff", 3.0),
    BELOW_AVERAGE("Below Average", 2.5),
    AVERAGE("Average", 2.0),
    FLEXIBLE("Flexible", 1.5),
    VERY_FLEXIBLE("Very Flexible", 1.0)
}
