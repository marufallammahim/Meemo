package com.meemo.nlu

import org.junit.Assert.assertEquals
import org.junit.Test

class InputNormalizerTest {
    @Test
    fun removesWakeWordsFillerAndNormalizesSpacing() {
        assertEquals("open browser", InputNormalizer().normalize("  Hey Meemo, could you OPEN   browser please! "))
    }

    @Test
    fun removesBengaliWakePhraseWithoutChangingTheCommand() {
        assertEquals("ব্যাটারি কত", InputNormalizer().normalize("হ্যালো মিমো, ব্যাটারি কত?"))
        assertEquals("ব্রাউজার খোলো", InputNormalizer().normalize("মিমো ব্রাউজার খোলো"))
    }

    @Test
    fun keepsBengaliLettersAndDigitsDuringNormalization() {
        assertEquals("ভলিউম ৫০", InputNormalizer().normalize("ভলিউম ৫০"))
    }
}
