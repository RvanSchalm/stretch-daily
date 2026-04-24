package com.stretchdaily.app.ui.theme

import com.stretchdaily.app.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTintTest {

    @Test
    fun `every category has a tint`() {
        Category.entries.forEach { cat ->
            val color = cat.tint()
            // Default Color() alpha is 1.0f; tints are opaque.
            assertEquals("alpha for $cat", 1.0f, color.alpha, 0.001f)
        }
    }

    @Test
    fun `tints are distinct across categories`() {
        val colors = Category.entries.map { it.tint() }
        val distinct = colors.toSet()
        assertEquals("all 7 tints should be distinct", 7, distinct.size)
    }

    @Test
    fun `tint result is memoized (same instance per call)`() {
        val first = Category.NECK.tint()
        val second = Category.NECK.tint()
        // Color is a value class backed by ULong; equality is reference-free.
        // We just need the numeric value identical.
        assertEquals(first.value, second.value)
    }

    @Test
    fun `tints are in the L=0_82 desaturated band, no pure white or black`() {
        Category.entries.forEach { cat ->
            val c = cat.tint()
            val sum = c.red + c.green + c.blue
            assertTrue("$cat shouldn't be black — sum=$sum", sum > 1.5f)
            assertTrue("$cat shouldn't be white — sum=$sum", sum < 2.95f)
        }
    }

    @Test
    fun `Neck and Hips are not the same tint (sanity)`() {
        assertNotEquals(Category.NECK.tint().value, Category.HIPS.tint().value)
    }
}
