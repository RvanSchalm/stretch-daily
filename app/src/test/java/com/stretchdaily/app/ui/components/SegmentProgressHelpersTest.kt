package com.stretchdaily.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentProgressHelpersTest {

    @Test
    fun `index before current is Past`() {
        assertEquals(SegmentState.Past, segmentStateFor(index = 0, currentIndex = 3, total = 5))
        assertEquals(SegmentState.Past, segmentStateFor(index = 2, currentIndex = 3, total = 5))
    }

    @Test
    fun `index equal to current is Current`() {
        assertEquals(SegmentState.Current, segmentStateFor(index = 3, currentIndex = 3, total = 5))
    }

    @Test
    fun `index after current is Future`() {
        assertEquals(SegmentState.Future, segmentStateFor(index = 4, currentIndex = 3, total = 5))
    }

    @Test
    fun `currentIndex equal to total means everything is Past`() {
        (0 until 5).forEach { i ->
            assertEquals(SegmentState.Past, segmentStateFor(index = i, currentIndex = 5, total = 5))
        }
    }

    @Test
    fun `negative currentIndex treats everything as Future`() {
        (0 until 5).forEach { i ->
            assertEquals(SegmentState.Future, segmentStateFor(index = i, currentIndex = -1, total = 5))
        }
    }
}
