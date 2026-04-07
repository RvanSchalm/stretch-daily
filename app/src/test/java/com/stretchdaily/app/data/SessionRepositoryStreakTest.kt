package com.stretchdaily.app.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM tests for [SessionRepository.computeStreak]. The streak math
 * deliberately lives in a companion-object function so it can be exercised
 * here without standing up Room.
 */
class SessionRepositoryStreakTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    /** 12:00 UTC on the given date — far enough from midnight to avoid edges. */
    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `empty history yields zero`() {
        val streak = SessionRepository.computeStreak(
            completionTimestamps = emptyList(),
            now = midday(2026, 4, 7),
            zoneId = zone,
        )
        assertEquals(0, streak)
    }

    @Test
    fun `single session today is streak of 1`() {
        val streak = SessionRepository.computeStreak(
            completionTimestamps = listOf(midday(2026, 4, 7)),
            now = midday(2026, 4, 7),
            zoneId = zone,
        )
        assertEquals(1, streak)
    }

    @Test
    fun `three consecutive days ending today is streak of 3`() {
        val streak = SessionRepository.computeStreak(
            completionTimestamps = listOf(
                midday(2026, 4, 5),
                midday(2026, 4, 6),
                midday(2026, 4, 7),
            ),
            now = midday(2026, 4, 7),
            zoneId = zone,
        )
        assertEquals(3, streak)
    }

    @Test
    fun `multiple sessions on same day count once`() {
        val streak = SessionRepository.computeStreak(
            completionTimestamps = listOf(
                midday(2026, 4, 6),
                midday(2026, 4, 7),
                midday(2026, 4, 7),
                midday(2026, 4, 7),
            ),
            now = midday(2026, 4, 7),
            zoneId = zone,
        )
        assertEquals(2, streak)
    }

    @Test
    fun `gap of one day breaks the streak from yesterday`() {
        // Last session was 4/5; today is 4/7; yesterday (4/6) is missing.
        val streak = SessionRepository.computeStreak(
            completionTimestamps = listOf(
                midday(2026, 4, 4),
                midday(2026, 4, 5),
            ),
            now = midday(2026, 4, 7),
            zoneId = zone,
        )
        assertEquals(0, streak)
    }

    @Test
    fun `yesterday-only history still counts via grace window`() {
        // No session today, but one yesterday — grace window keeps the
        // streak alive so opening the app late doesn't punish the user.
        val streak = SessionRepository.computeStreak(
            completionTimestamps = listOf(
                midday(2026, 4, 5),
                midday(2026, 4, 6),
            ),
            now = midday(2026, 4, 7),
            zoneId = zone,
        )
        assertEquals(2, streak)
    }

    @Test
    fun `older block of consecutive days does not count if today and yesterday are missing`() {
        val streak = SessionRepository.computeStreak(
            completionTimestamps = listOf(
                midday(2026, 3, 1),
                midday(2026, 3, 2),
                midday(2026, 3, 3),
                midday(2026, 3, 4),
            ),
            now = midday(2026, 4, 7),
            zoneId = zone,
        )
        assertEquals(0, streak)
    }
}
