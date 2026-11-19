package com.meemo.nlu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuzzyMatcherTest {
    @Test
    fun ranksExactCandidateFirst() {
        val ranked = FuzzyMatcher().rank("chrome", listOf("Firefox", "Chrome", "Camera")) { it }
        assertEquals("Chrome", ranked.first().value)
        assertEquals(1f, ranked.first().score)
    }

    @Test
    fun rejectsCandidatesBelowThreshold() {
        assertNull(FuzzyMatcher().bestMatch("unrelated", listOf("Chrome")) { it })
    }
}
