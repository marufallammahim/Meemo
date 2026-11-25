package com.marufallammahim.meemo.assistant.parser

import com.marufallammahim.meemo.assistant.command.CommandInputSource
import com.marufallammahim.meemo.assistant.command.DetectedLanguage
import com.marufallammahim.meemo.core.utilities.TimeProvider
import java.text.Normalizer
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
    fun `strips supported English wake phrase variants case insensitively`() {
        listOf(
            "Hey Meemo, open Chrome",
            "HI MEEMO! open Chrome",
            "Okay Meemo: open Chrome"
        ).forEach { input ->
            val result = normalizer.normalize(input)
            assertTrue(result.wakePhraseDetected)
            assertEquals("open Chrome", result.normalizedText)
        }
    }

    @Test
    fun `strips speech recognition punctuation between English wake phrase words`() {
        listOf(
            "Hey, Meemo, open Chrome",
            "Hello! Meemo: open Chrome",
            "Okay. Meemo; open Chrome"
        ).forEach { input ->
            val result = normalizer.normalize(input)
            assertTrue(result.wakePhraseDetected)
            assertEquals("open Chrome", result.normalizedText)
        }
    }

    @Test
    fun `strips Bengali wake phrase and converts Bengali numerals to ASCII`() {
        val result = normalizer.normalize("হ্যালো মিমো, ভলিউম ৫০ শতাংশে সেট করো।")
        assertTrue(result.wakePhraseDetected)
        assertEquals("ভলিউম 50 শতাংশে সেট করো", result.normalizedText)
        assertEquals(DetectedLanguage.BENGALI, result.detectedLanguage)
    }

    @Test
    fun `strips Bengali speech recognition punctuation between wake phrase words`() {
        val result = normalizer.normalize("হ্যালো, মিমো। ব্যাটারি কত?")
        assertTrue(result.wakePhraseDetected)
        assertEquals("ব্যাটারি কত", result.normalizedText)
    }

    @Test
    fun `normalizes decomposed Bengali text while preserving the raw transcript`() {
        val raw = Normalizer.normalize(
            "হ্যালো মিমো, ব্যাটারি কত?",
            Normalizer.Form.NFD
        )

        val result = normalizer.normalize(raw)

        assertEquals(raw, result.rawText)
        assertTrue(result.wakePhraseDetected)
        assertEquals("ব্যাটারি কত", result.normalizedText)
    }

    @Test
    fun `detects mixed Banglish command without wake phrase`() {
        val result = normalizer.normalize("Chrome kholo")
        assertFalse(result.wakePhraseDetected)
        assertEquals("Chrome kholo", result.normalizedText)
        assertEquals(DetectedLanguage.MIXED_BANGLISH, result.detectedLanguage)
    }

    @Test
    fun `cleans outer punctuation while preserving dots and hyphens in package names`() {
        val result = normalizer.normalize("  , open com.example.my-app.beta!  ")

        assertEquals("open com.example.my-app.beta", result.normalizedText)
    }

    @Test
    fun `collapses repeated whitespace between command words`() {
        val result = normalizer.normalize("open     the    camera")

        assertEquals("open the camera", result.normalizedText)
    }

    @Test
    fun `handles empty and whitespace only input safely`() {
        val result = normalizer.normalize("   ...   ")
        assertEquals("", result.normalizedText)
    }

    @Test
    fun `detects Banglish markers as whole words`() {
        assertEquals(DetectedLanguage.MIXED_BANGLISH, languageResolver.detectLanguage("volume komao"))
        assertEquals(DetectedLanguage.ENGLISH, languageResolver.detectLanguage("the word kholona is not a command marker"))
        assertEquals(DetectedLanguage.BENGALI, languageResolver.detectLanguage("শুধু বাংলা"))
        assertEquals(DetectedLanguage.MIXED_BANGLISH, languageResolver.detectLanguage("বাংলা with English"))
    }

    @Test
    fun `converts every Bengali numeral while preserving other characters`() {
        assertEquals(
            "0123456789 - 50%",
            languageResolver.convertBengaliDigitsToAscii("০১২৩৪৫৬৭৮৯ - ৫০%")
        )
    }
}
