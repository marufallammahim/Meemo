package com.meemo.nlu

import org.junit.Assert.assertEquals
import org.junit.Test

class InputNormalizerTest {
    @Test
    fun removesWakeWordsFillerAndNormalizesSpacing() {
        assertEquals("open browser", InputNormalizer().normalize("  Hey Meemo, could you OPEN   browser please! "))
    }
}
