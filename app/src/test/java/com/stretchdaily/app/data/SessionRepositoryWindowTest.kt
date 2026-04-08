package com.stretchdaily.app.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM tests for [SessionRepository.countSessionsInLastDays]. Verifies the
 * trailing-window math used by the dashboard "this week" KPI card.
 */
class SessionRepositoryWindowTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `empty history yields zero`() {
        val count = SessionRepository.countSessionsInLastDays(
            completionTimestamps = emptyList(),
            now = midday(2026, 4, 7),
            days = 7,
            zoneId = zone,
        )
        assertEquals(0, count)
    }

    @Test
    fun `today counts when window is one day`() {
        val count = SessionRepository.countSessionsInLastDays(
            completionTimestamps = listOf(midday(2026, 4, 7)),
            now = midday(2026, 4, 7),
            days = 1,
            zoneId = zone,
        )
        assertEquals(1, count)
    }

    @Test
    fun `seven day window includes today and the prior six days`() {
        // Sessions on every day of the trailing 7-day window.
        val timestamps = (1..7).map { midday(2026, 4, it) }
        val count = SessionRepository.countSessionsInLastDays(
            completionTimestamps = timestamps,
            now = midday(2026, 4, 7),
            days = 7,
            zoneId = zone,
        )
        assertEquals(7, count)
    }

    @Test
    fun `session eight days ago is excluded from the seven day window`() {
        val count = SessionRepository.countSessionsInLastDays(
            completionTimestamps = listOf(midday(2026, 3, 31)),
            now = midday(2026, 4, 7),
            days = 7,
            zoneId = zone,
        )
        assertEquals(0, count)
    }

    @Test
    fun `multiple sessions on the same day each count`() {
        // Two completed sessions on the same day — both must count individually
        // for the volume KPI to feel accurate.
        val timestamps = listOf(
            midday(2026, 4, 7),
            midday(2026, 4, 7),
            midday(2026, 4, 5),
        )
        val count = SessionRepository.countSessionsInLastDays(
            completionTimestamps = timestamps,
            now = midday(2026, 4, 7),
            days = 7,
            zoneId = zone,
        )
        assertEquals(3, count)
    }

    @Test
    fun `future session is excluded`() {
        val count = SessionRepository.countSessionsInLastDays(
            completionTimestamps = listOf(midday(2026, 4, 9)),
            now = midday(2026, 4, 7),
            days = 7,
            zoneId = zone,
        )
        assertEquals(0, count)
    }
}
