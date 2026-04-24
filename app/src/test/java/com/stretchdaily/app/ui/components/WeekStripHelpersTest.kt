package com.stretchdaily.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekStripHelpersTest {

    @Test
    fun `weekOf returns Monday-start dates for a Wednesday`() {
        val days = weekOf(LocalDate.of(2026, 4, 22))
        val expected = listOf(
            LocalDate.of(2026, 4, 20),
            LocalDate.of(2026, 4, 21),
            LocalDate.of(2026, 4, 22),
            LocalDate.of(2026, 4, 23),
            LocalDate.of(2026, 4, 24),
            LocalDate.of(2026, 4, 25),
            LocalDate.of(2026, 4, 26),
        )
        assertEquals(expected, days)
    }

    @Test
    fun `weekOf on a Monday returns that Monday as the first day`() {
        val days = weekOf(LocalDate.of(2026, 4, 20))
        assertEquals(LocalDate.of(2026, 4, 20), days.first())
        assertEquals(7, days.size)
    }

    @Test
    fun `weekOf on a Sunday returns the preceding Monday as first`() {
        val days = weekOf(LocalDate.of(2026, 4, 26))
        assertEquals(LocalDate.of(2026, 4, 20), days.first())
        assertEquals(LocalDate.of(2026, 4, 26), days.last())
    }

    @Test
    fun `dayStateFor marks today as Today when not in completed set`() {
        val today = LocalDate.of(2026, 4, 22)
        val state = dayStateFor(day = today, today = today, completed = emptySet())
        assertEquals(DayState.Today, state)
    }

    @Test
    fun `dayStateFor marks today as Completed when in completed set`() {
        val today = LocalDate.of(2026, 4, 22)
        val state = dayStateFor(day = today, today = today, completed = setOf(today))
        assertEquals(DayState.Completed, state)
    }

    @Test
    fun `dayStateFor marks past day as Completed when in set and Idle otherwise`() {
        val today = LocalDate.of(2026, 4, 22)
        val past = LocalDate.of(2026, 4, 20)
        assertEquals(DayState.Completed, dayStateFor(past, today, setOf(past)))
        assertEquals(DayState.Idle, dayStateFor(past, today, emptySet()))
    }

    @Test
    fun `dayStateFor marks future day as Idle regardless of completed set`() {
        val today = LocalDate.of(2026, 4, 22)
        val future = LocalDate.of(2026, 4, 26)
        assertEquals(DayState.Idle, dayStateFor(future, today, setOf(future)))
    }
}
