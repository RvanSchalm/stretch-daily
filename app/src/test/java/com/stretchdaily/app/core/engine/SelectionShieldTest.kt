package com.stretchdaily.app.core.engine

import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionShieldTest {

    private val shield = SelectionShield()

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val fortnight = 14 * day

    private fun exercise(id: String, lastPerformed: Long?) = Exercise(
        id = id,
        name = id,
        category = Category.NECK,
        cues = emptyList(),
        isUnilateral = false,
        isTimed = true,
        targetReps = null,
        secondsPerRep = null,
        totalTime = 60,
        lastPerformed = lastPerformed,
    )

    @Test
    fun `exercise never performed is stale`() {
        val stale = shield.staleExercises(listOf(exercise("e1", null)), now)
        assertEquals(1, stale.size)
        assertEquals("e1", stale.first().id)
    }

    @Test
    fun `exercise performed yesterday is not stale`() {
        val stale = shield.staleExercises(listOf(exercise("e1", now - day)), now)
        assertTrue(stale.isEmpty())
    }

    @Test
    fun `exercise performed exactly 14 days ago is not stale`() {
        // Boundary check: cutoff = now - 14d, predicate uses strict <, so equal timestamps stay fresh.
        val stale = shield.staleExercises(listOf(exercise("e1", now - fortnight)), now)
        assertTrue(stale.isEmpty())
    }

    @Test
    fun `exercise performed 15 days ago is stale`() {
        val stale = shield.staleExercises(listOf(exercise("e1", now - 15 * day)), now)
        assertEquals(1, stale.size)
    }

    @Test
    fun `mixed pool returns only the stale ones`() {
        val pool = listOf(
            exercise("fresh", now - day),
            exercise("stale-old", now - 30 * day),
            exercise("never", null),
            exercise("border", now - fortnight),
        )

        val stale = shield.staleExercises(pool, now).map { it.id }.toSet()

        assertEquals(setOf("stale-old", "never"), stale)
    }

    @Test
    fun `custom staleAfterMillis is honored`() {
        val pool = listOf(
            exercise("five-days-ago", now - 5 * day),
            exercise("two-days-ago", now - 2 * day),
        )

        val stale = shield
            .staleExercises(pool, now, staleAfterMillis = 3 * day)
            .map { it.id }
            .toSet()

        assertEquals(setOf("five-days-ago"), stale)
    }
}
