package com.stretchdaily.app.core.benchmark

import org.junit.Assert.assertEquals
import org.junit.Test

class BenchmarkBetterTest {

    @Test
    fun `ascending benchmarks resolve to HIGHER`() {
        assertEquals(Better.HIGHER, benchmarkBetter("BM_CERVICAL_ROTATION"))
        assertEquals(Better.HIGHER, benchmarkBetter("BM_THORACIC_ROTATION"))
        assertEquals(Better.HIGHER, benchmarkBetter("BM_KNEE_TO_WALL"))
        assertEquals(Better.HIGHER, benchmarkBetter("BM_WRIST_EXTENSION"))
        assertEquals(Better.HIGHER, benchmarkBetter("BM_WRIST_FLEXION"))
    }

    @Test
    fun `descending benchmarks resolve to LOWER`() {
        assertEquals(Better.LOWER, benchmarkBetter("BM_APLEY_SCRATCH"))
        assertEquals(Better.LOWER, benchmarkBetter("BM_BUTTERFLY"))
        assertEquals(Better.LOWER, benchmarkBetter("BM_SIT_AND_REACH"))
        assertEquals(Better.LOWER, benchmarkBetter("BM_THOMAS_TEST"))
    }

    @Test
    fun `categorical benchmark resolves to CATEGORICAL`() {
        assertEquals(Better.CATEGORICAL, benchmarkBetter("BM_ATG_SPLIT_SQUAT"))
    }

    @Test
    fun `unknown benchmark id resolves to CATEGORICAL as a safe default`() {
        assertEquals(Better.CATEGORICAL, benchmarkBetter("BM_MYSTERY"))
    }
}
