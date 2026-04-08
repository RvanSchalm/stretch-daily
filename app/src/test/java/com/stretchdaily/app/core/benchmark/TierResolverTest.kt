package com.stretchdaily.app.core.benchmark

import com.stretchdaily.app.core.model.FlexibilityTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TierResolverTest {

    private val resolver = TierResolver()

    // --- Ascending benchmarks ---

    @Test
    fun `cervical rotation maps degrees to tiers`() {
        assertEquals(FlexibilityTier.STIFF, resolver.resolve("BM_CERVICAL_ROTATION", 45.0))
        assertEquals(FlexibilityTier.BELOW_AVERAGE, resolver.resolve("BM_CERVICAL_ROTATION", 60.0))
        assertEquals(FlexibilityTier.AVERAGE, resolver.resolve("BM_CERVICAL_ROTATION", 75.0))
        assertEquals(FlexibilityTier.FLEXIBLE, resolver.resolve("BM_CERVICAL_ROTATION", 85.0))
        assertEquals(FlexibilityTier.VERY_FLEXIBLE, resolver.resolve("BM_CERVICAL_ROTATION", 95.0))
    }

    @Test
    fun `ascending breakpoint values fall into the higher-flex tier`() {
        // value exactly at 50 is not < 50, so it belongs to BELOW_AVERAGE.
        assertEquals(FlexibilityTier.BELOW_AVERAGE, resolver.resolve("BM_CERVICAL_ROTATION", 50.0))
        assertEquals(FlexibilityTier.AVERAGE, resolver.resolve("BM_CERVICAL_ROTATION", 70.0))
        assertEquals(FlexibilityTier.VERY_FLEXIBLE, resolver.resolve("BM_CERVICAL_ROTATION", 90.0))
    }

    @Test
    fun `knee-to-wall uses its own breakpoints`() {
        assertEquals(FlexibilityTier.STIFF, resolver.resolve("BM_KNEE_TO_WALL", 3.0))
        assertEquals(FlexibilityTier.AVERAGE, resolver.resolve("BM_KNEE_TO_WALL", 9.0))
        assertEquals(FlexibilityTier.VERY_FLEXIBLE, resolver.resolve("BM_KNEE_TO_WALL", 14.0))
    }

    @Test
    fun `wrist extension and flexion use different breakpoints`() {
        // Extension: 60/70/80/90. Flexion: 65/75/85/95.
        assertEquals(FlexibilityTier.BELOW_AVERAGE, resolver.resolve("BM_WRIST_EXTENSION", 65.0))
        assertEquals(FlexibilityTier.STIFF, resolver.resolve("BM_WRIST_FLEXION", 65.0))
    }

    // --- Descending benchmarks ---

    @Test
    fun `apley scratch maps cm gap inversely`() {
        // > 15 cm gap -> stiff
        assertEquals(FlexibilityTier.STIFF, resolver.resolve("BM_APLEY_SCRATCH", 20.0))
        assertEquals(FlexibilityTier.BELOW_AVERAGE, resolver.resolve("BM_APLEY_SCRATCH", 12.0))
        assertEquals(FlexibilityTier.AVERAGE, resolver.resolve("BM_APLEY_SCRATCH", 7.0))
        assertEquals(FlexibilityTier.FLEXIBLE, resolver.resolve("BM_APLEY_SCRATCH", 2.0))
        // Finger overlap = negative gap = very flexible.
        assertEquals(FlexibilityTier.VERY_FLEXIBLE, resolver.resolve("BM_APLEY_SCRATCH", -3.0))
    }

    @Test
    fun `sit and reach goes negative for flexible`() {
        assertEquals(FlexibilityTier.STIFF, resolver.resolve("BM_SIT_AND_REACH", 25.0))
        assertEquals(FlexibilityTier.BELOW_AVERAGE, resolver.resolve("BM_SIT_AND_REACH", 15.0))
        assertEquals(FlexibilityTier.AVERAGE, resolver.resolve("BM_SIT_AND_REACH", 5.0))
        assertEquals(FlexibilityTier.FLEXIBLE, resolver.resolve("BM_SIT_AND_REACH", -5.0))
        assertEquals(FlexibilityTier.VERY_FLEXIBLE, resolver.resolve("BM_SIT_AND_REACH", -15.0))
    }

    @Test
    fun `thomas test uses signed degrees where positive is stiff`() {
        assertEquals(FlexibilityTier.STIFF, resolver.resolve("BM_THOMAS_TEST", 20.0))
        assertEquals(FlexibilityTier.BELOW_AVERAGE, resolver.resolve("BM_THOMAS_TEST", 10.0))
        assertEquals(FlexibilityTier.AVERAGE, resolver.resolve("BM_THOMAS_TEST", 0.0))
        assertEquals(FlexibilityTier.FLEXIBLE, resolver.resolve("BM_THOMAS_TEST", -7.0))
        assertEquals(FlexibilityTier.VERY_FLEXIBLE, resolver.resolve("BM_THOMAS_TEST", -15.0))
    }

    @Test
    fun `butterfly maps cm to floor inversely`() {
        assertEquals(FlexibilityTier.STIFF, resolver.resolve("BM_BUTTERFLY", 25.0))
        assertEquals(FlexibilityTier.VERY_FLEXIBLE, resolver.resolve("BM_BUTTERFLY", 3.0))
    }

    @Test
    fun `descending breakpoint values fall into the stiffer tier`() {
        // Descending: value exactly at 20 is not > 20, so moves to BELOW_AVERAGE.
        assertEquals(FlexibilityTier.BELOW_AVERAGE, resolver.resolve("BM_SIT_AND_REACH", 20.0))
        // Exactly at -10 is not > -10, so VERY_FLEXIBLE.
        assertEquals(FlexibilityTier.VERY_FLEXIBLE, resolver.resolve("BM_SIT_AND_REACH", -10.0))
    }

    // --- Unknown / categorical IDs ---

    @Test
    fun `unknown id returns null`() {
        assertNull(resolver.resolve("BM_DOES_NOT_EXIST", 50.0))
    }

    @Test
    fun `categorical ATG Split Squat is not numerically resolvable`() {
        assertNull(resolver.resolve("BM_ATG_SPLIT_SQUAT", 50.0))
        assertFalse(resolver.handles("BM_ATG_SPLIT_SQUAT"))
    }

    @Test
    fun `handles returns true for all numeric benchmark ids`() {
        val numericIds = listOf(
            "BM_CERVICAL_ROTATION", "BM_THORACIC_ROTATION", "BM_KNEE_TO_WALL",
            "BM_WRIST_EXTENSION", "BM_WRIST_FLEXION", "BM_APLEY_SCRATCH",
            "BM_BUTTERFLY", "BM_SIT_AND_REACH", "BM_THOMAS_TEST",
        )
        for (id in numericIds) {
            assertTrue("expected $id to be resolvable", resolver.handles(id))
        }
    }
}
