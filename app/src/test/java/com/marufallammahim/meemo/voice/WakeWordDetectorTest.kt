package com.marufallammahim.meemo.voice

import com.marufallammahim.meemo.core.permissions.PermissionChecker
import io.mockk.mockk
import java.text.Normalizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordDetectorTest {
    private val detector = WakeWordDetector(mockk<PermissionChecker>(relaxed = true))

    @Test
    fun recognizesEnglishAndBengaliWakePhrasesBeforeCommands() {
        assertTrue(detector.containsWakePhrase("Hey, Meemo, open browser"))
        assertTrue(detector.containsWakePhrase("Hello Meemo! battery status"))
        assertTrue(detector.containsWakePhrase("হ্যালো, মিমো। ব্যাটারি কত?"))
        assertTrue(detector.containsWakePhrase("হাই মিমো, ব্রাউজার খোলো"))
        assertTrue(detector.containsWakePhrase("মিমো, ভলিউম বাড়াও"))
    }

    @Test
    fun recognizesCanonicallyDecomposedBengaliTranscript() {
        val decomposed = Normalizer.normalize(
            "হ্যালো মিমো, ব্যাটারি কত?",
            Normalizer.Form.NFD
        )

        assertTrue(detector.containsWakePhrase(decomposed))
    }

    @Test
    fun rejectsWordsThatOnlyBeginWithWakePhraseLetters() {
        assertFalse(detector.containsWakePhrase("Meemology is a word"))
        assertFalse(detector.containsWakePhrase("Hey Meemometer"))
        assertFalse(detector.containsWakePhrase("মিমোরা এসেছে"))
        assertFalse(detector.containsWakePhrase("please say hello meemo"))
    }
}
