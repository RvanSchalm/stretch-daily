package com.stretchdaily.app.core.util

/**
 * Tiny seam over `System.currentTimeMillis()` so engine code that needs "now"
 * can be unit-tested without freezing real time. The production binding lives
 * in `EngineModule`; tests inject a fake.
 */
fun interface Clock {
    fun now(): Long
}
