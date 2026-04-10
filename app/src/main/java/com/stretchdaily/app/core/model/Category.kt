package com.stretchdaily.app.core.model

import kotlinx.serialization.Serializable

/**
 * The seven body areas covered by Stretch Daily.
 *
 * Each exercise and benchmark belongs to exactly one category. The Longevity
 * Engine uses category-level weights (derived from benchmark tiers) to bias
 * exercise selection toward the user's stiffest areas.
 */
@Serializable
enum class Category(val displayName: String) {
    NECK("Neck"),
    SHOULDERS("Shoulders"),
    WRISTS("Wrists"),
    SPINE("Spine"),
    HIPS("Hips"),
    KNEES("Knees"),
    ANKLES("Ankles")
}
