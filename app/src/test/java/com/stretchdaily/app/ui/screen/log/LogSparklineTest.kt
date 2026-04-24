package com.stretchdaily.app.ui.screen.log

import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSparklineTest {

    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, zone)
        .toInstant().toEpochMilli()

    private fun log(tier: FlexibilityTier, daysAgo: Long) = BenchmarkLog(
        id = 0L,
        benchmarkId = "BM",
        rawValue = tier.name,
        resolvedTier = tier,
        loggedAt = Instant.ofEpochMilli(now).minusSeconds(daysAgo * 86_400).toEpochMilli(),
    )

    @Test
    fun `empty input returns empty list`() {
        assertTrue(sparklineValues(emptyList(), now).isEmpty())
    }

    @Test
    fun `single log returns list with one normalized value`() {
        val values = sparklineValues(listOf(log(FlexibilityTier.AVERAGE, 10)), now)
        assertEquals(1, values.size)
        // AVERAGE is the middle of 5 tiers: ordinal math maps it to 0.5.
        assertEquals(0.5, values.single(), 0.001)
    }

    @Test
    fun `tier extremes map to 0 and 1`() {
        val values = sparklineValues(
            listOf(
                log(FlexibilityTier.STIFF, 30),
                log(FlexibilityTier.VERY_FLEXIBLE, 5),
            ),
            now,
        )
        assertEquals(listOf(0.0, 1.0), values)
    }

    @Test
    fun `logs older than 6 months are dropped`() {
        val values = sparklineValues(
            listOf(
                // 220 days back — outside window
                log(FlexibilityTier.STIFF, 220),
                // 30 days back — inside window
                log(FlexibilityTier.VERY_FLEXIBLE, 30),
            ),
            now,
        )
        assertEquals(listOf(1.0), values)
    }

    @Test
    fun `values are sorted oldest first`() {
        val values = sparklineValues(
            listOf(
                // emitted newest-first, but helper should re-sort
                log(FlexibilityTier.VERY_FLEXIBLE, 5),
                log(FlexibilityTier.STIFF, 90),
                log(FlexibilityTier.AVERAGE, 30),
            ),
            now,
        )
        // oldest (90 d) first, then 30 d, then 5 d
        assertEquals(listOf(0.0, 0.5, 1.0), values)
    }
}
