package com.marufallammahim.meemo.assistant.parser

import com.marufallammahim.meemo.assistant.command.CommandInputSource
import com.marufallammahim.meemo.assistant.command.DetectedLanguage
import com.marufallammahim.meemo.core.utilities.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommandNormalizerTest {

    private lateinit var languageResolver: LanguageResolver
    private lateinit var normalizer: CommandNormalizer

    private val fakeTimeProvider = object : TimeProvider {
        override fun nowMillis(): Long = 1_700_000_000_000L
        override fun currentHourOfDay(): Int = 14
    }

    @Before
    fun setUp() {
        languageResolver = LanguageResolver()
        normalizer = CommandNormalizer(languageResolver, fakeTimeProvider)
    }

    @Test
    fun `strips English wake phrase Hello Meemo and preserves command`() {
        val result = normalizer.normalize(
            "  Hello Meemo, turn on the flashlight!  ",
            CommandInputSource.VOICE_WAKE_WORD
        )
        assertTrue(result.wakePhraseDetected)
        assertEquals("turn on the flashlight", result.normalizedText)
        assertEquals(DetectedLanguage.ENGLISH, result.detectedLanguage)
    }

    @Test
    fun `strips Bengali wake phrase and converts Bengali numerals to ASCII`() {
        val result = normalizer.normalize("হ্যালো মিমো, ভলিউম ৫০ শতাংশে সেট করো।")
        assertTrue(result.wakePhraseDetected)
        assertEquals("ভলিউম 50 শতাংশে সেট করো", result.normalizedText)
        assertEquals(DetectedLanguage.BENGALI, result.detectedLanguage)
    }

    @Test
    fun `detects mixed Banglish command without wake phrase`() {
        val result = normalizer.normalize("Chrome kholo")
        assertFalse(result.wakePhraseDetected)
        assertEquals("Chrome kholo", result.normalizedText)
        assertEquals(DetectedLanguage.MIXED_BANGLISH, result.detectedLanguage)
    }

    @Test
    fun `handles empty and whitespace-only input safely`() {
        val result = normalizer.normalize("   ...   ")
        assertEquals("", result.normalizedText)
    }
}
