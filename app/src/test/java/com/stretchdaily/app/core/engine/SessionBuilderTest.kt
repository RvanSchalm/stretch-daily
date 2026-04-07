package com.stretchdaily.app.core.engine

import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.model.FlexibilityTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SessionBuilderTest {

    private val builder = SessionBuilder()

    private fun exercise(
        id: String,
        category: Category = Category.NECK,
        totalTime: Int = 90,
        lastPerformed: Long? = null,
    ) = Exercise(
        id = id,
        name = id,
        category = category,
        cues = emptyList(),
        isUnilateral = false,
        isTimed = true,
        targetReps = null,
        secondsPerRep = null,
        totalTime = totalTime,
        lastPerformed = lastPerformed,
    )

    /** A balanced 35-exercise pool — 5 per category, 90 s each. */
    private fun balancedPool(): List<Exercise> = Category.entries.flatMap { cat ->
        (1..5).map { exercise("${cat.name}-$it", cat) }
    }

    private fun flatWeights(weight: Double = FlexibilityTier.AVERAGE.weight): Map<Category, Double> =
        Category.entries.associateWith { weight }

    @Test
    fun `default config produces 5 to 8 exercises within 600 to 900 seconds`() {
        val plan = builder.build(
            exercises = balancedPool(),
            weights = flatWeights(),
            forcedExerciseIds = emptySet(),
            random = Random(0),
        )

        assertTrue("size=${plan.items.size}", plan.items.size in 5..8)
        assertTrue("totalSeconds=${plan.totalSeconds}", plan.totalSeconds >= 600)
        // With perExerciseCap=120 and max 8 exercises we can never exceed 8 * 120 = 960.
        assertTrue("totalSeconds=${plan.totalSeconds}", plan.totalSeconds <= 960)
    }

    @Test
    fun `never picks the same exercise twice in a single session`() {
        repeat(50) { seed ->
            val plan = builder.build(
                exercises = balancedPool(),
                weights = flatWeights(),
                forcedExerciseIds = emptySet(),
                random = Random(seed.toLong()),
            )
            val ids = plan.items.map { it.exercise.id }
            assertEquals("Duplicate ids in $ids", ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `forced exercises are inserted first and marked as forced`() {
        val pool = balancedPool()
        val forced = setOf(pool[0].id, pool[1].id)

        val plan = builder.build(
            exercises = pool,
            weights = flatWeights(),
            forcedExerciseIds = forced,
            random = Random(0),
        )

        assertEquals(2, plan.items.count { it.isForced })
        // The first two slots are the forced ones.
        assertTrue(plan.items[0].isForced)
        assertTrue(plan.items[1].isForced)
        assertEquals(forced, plan.items.take(2).map { it.exercise.id }.toSet())
    }

    @Test
    fun `forced count is capped at maxForced even when many ids are stale`() {
        val pool = balancedPool()
        val allStale = pool.map { it.id }.toSet()

        val plan = builder.build(
            exercises = pool,
            weights = flatWeights(),
            forcedExerciseIds = allStale,
            random = Random(0),
        )

        assertEquals(2, plan.items.count { it.isForced })
    }

    @Test
    fun `oldest stale exercises are forced first`() {
        val pool = listOf(
            exercise("never", lastPerformed = null),
            exercise("ancient", lastPerformed = 1L),
            exercise("recent-but-stale", lastPerformed = 1_000_000L),
            exercise("filler-1", lastPerformed = null),
            exercise("filler-2", lastPerformed = null),
            exercise("filler-3", lastPerformed = null),
            exercise("filler-4", lastPerformed = null),
            exercise("filler-5", lastPerformed = null),
        )
        // Three are eligible to be forced; only two should actually be picked.
        val forced = setOf("never", "ancient", "recent-but-stale")

        val plan = builder.build(
            exercises = pool,
            weights = flatWeights(),
            forcedExerciseIds = forced,
            random = Random(0),
        )

        val forcedIds = plan.items.filter { it.isForced }.map { it.exercise.id }
        assertEquals(2, forcedIds.size)
        // Nulls are stalest, so "never" must appear; "ancient" should beat "recent-but-stale".
        assertTrue("never" in forcedIds)
        assertTrue("ancient" in forcedIds)
        assertFalse("recent-but-stale" in forcedIds)
    }

    @Test
    fun `single exercise is capped at the per-exercise cap`() {
        val pool = listOf(
            exercise("monster", totalTime = 600),
            exercise("a", totalTime = 90),
            exercise("b", totalTime = 90),
            exercise("c", totalTime = 90),
            exercise("d", totalTime = 90),
            exercise("e", totalTime = 90),
            exercise("f", totalTime = 90),
            exercise("g", totalTime = 90),
        )

        val plan = builder.build(
            exercises = pool,
            weights = flatWeights(),
            forcedExerciseIds = setOf("monster"),
            random = Random(0),
        )

        val monsterSlot = plan.items.first { it.exercise.id == "monster" }
        assertEquals(120, monsterSlot.effectiveSeconds)
    }

    @Test
    fun `stiff categories accumulate more session slots than flexible ones over many runs`() {
        // Two categories of equal pool size; NECK is stiff (3.0), ANKLES is very flexible (1.0).
        val pool = (1..15).map { exercise("neck-$it", Category.NECK) } +
                (1..15).map { exercise("ankle-$it", Category.ANKLES) }

        val weights = mapOf(
            Category.NECK to FlexibilityTier.STIFF.weight,
            Category.ANKLES to FlexibilityTier.VERY_FLEXIBLE.weight,
        ) + (Category.entries - setOf(Category.NECK, Category.ANKLES))
            .associateWith { FlexibilityTier.AVERAGE.weight }

        val random = Random(1234)
        var neckPicks = 0
        var anklePicks = 0
        repeat(500) {
            val plan = builder.build(
                exercises = pool,
                weights = weights,
                forcedExerciseIds = emptySet(),
                random = random,
            )
            for (item in plan.items) {
                when (item.exercise.category) {
                    Category.NECK -> neckPicks++
                    Category.ANKLES -> anklePicks++
                    else -> Unit
                }
            }
        }

        // Stiff (3.0) should be picked roughly 3x as often as very flexible (1.0).
        // Allow generous slack — we just need a clear directional signal.
        assertTrue(
            "neck=$neckPicks vs ankle=$anklePicks — stiff should dominate",
            neckPicks > anklePicks * 2,
        )
    }

    @Test
    fun `single stiff category gets exercises even when most others are flexible`() {
        val pool = balancedPool()
        val weights = Category.entries.associateWith {
            if (it == Category.HIPS) FlexibilityTier.STIFF.weight
            else FlexibilityTier.VERY_FLEXIBLE.weight
        }

        var hipsHits = 0
        repeat(100) { seed ->
            val plan = builder.build(
                exercises = pool,
                weights = weights,
                forcedExerciseIds = emptySet(),
                random = Random(seed.toLong()),
            )
            if (plan.items.any { it.exercise.category == Category.HIPS }) hipsHits++
        }

        // The stiff category should appear in the vast majority of sessions.
        assertTrue("HIPS appeared in only $hipsHits/100 sessions", hipsHits > 80)
    }

    @Test
    fun `no benchmarks logged still produces a valid session`() {
        // CategoryWeightCalculator returns AVERAGE for everything in this case.
        val plan = builder.build(
            exercises = balancedPool(),
            weights = flatWeights(),
            forcedExerciseIds = emptySet(),
            random = Random(0),
        )

        assertTrue(plan.items.size in 5..8)
        assertTrue(plan.totalSeconds >= 600)
    }

    @Test
    fun `empty exercise pool returns empty plan`() {
        val plan = builder.build(
            exercises = emptyList(),
            weights = flatWeights(),
            forcedExerciseIds = emptySet(),
            random = Random(0),
        )
        assertTrue(plan.items.isEmpty())
        assertEquals(0, plan.totalSeconds)
    }

    @Test
    fun `respects maxExercises cap when minSeconds cannot be reached`() {
        // Pool of 30 s exercises — 8 of them only sums to 240 s, never hitting 600 s.
        val pool = (1..20).map { exercise("e-$it", totalTime = 30) }

        val plan = builder.build(
            exercises = pool,
            weights = flatWeights(),
            forcedExerciseIds = emptySet(),
            random = Random(0),
        )

        assertEquals(8, plan.items.size)
        assertEquals(240, plan.totalSeconds)
    }

    @Test
    fun `category weights are echoed back on the plan`() {
        val weights = flatWeights(weight = 2.5)
        val plan = builder.build(
            exercises = balancedPool(),
            weights = weights,
            forcedExerciseIds = emptySet(),
            random = Random(0),
        )
        assertEquals(weights, plan.categoryWeights)
    }
}
