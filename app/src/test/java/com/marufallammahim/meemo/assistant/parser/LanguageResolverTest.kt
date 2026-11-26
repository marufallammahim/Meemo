package com.marufallammahim.meemo.assistant.parser

import com.marufallammahim.meemo.assistant.command.DetectedLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageResolverTest {

    private val resolver = LanguageResolver()

    @Test
    fun `detects Bengali and mixed Bengali Latin input`() {
        assertEquals(DetectedLanguage.BENGALI, resolver.detectLanguage("বাংলা কমান্ড"))
        assertEquals(DetectedLanguage.MIXED_BANGLISH, resolver.detectLanguage("বাংলা command"))
    }

    @Test
    fun `detects Banglish using standalone command markers`() {
        assertEquals(DetectedLanguage.MIXED_BANGLISH, resolver.detectLanguage("Chrome kholo"))
        assertEquals(DetectedLanguage.ENGLISH, resolver.detectLanguage("the word kholopen"))
    }

    @Test
    fun `detects Banglish media transport verbs as whole words`() {
        listOf("gaan chalao", "gan bajao", "music bondho koro", "gaan thamaw", "porer gaan", "ager gan")
            .forEach { input ->
                assertEquals(DetectedLanguage.MIXED_BANGLISH, resolver.detectLanguage(input))
            }
        assertEquals(DetectedLanguage.ENGLISH, resolver.detectLanguage("the word chalaover is not a command"))
    }

    @Test
    fun `converts every Bengali digit without changing other characters`() {
        assertEquals(
            "0123456789 - 10%",
            resolver.convertBengaliDigitsToAscii("০১২৩৪৫৬৭৮৯ - ১০%")
        )
    }

    @Test
    fun `preserves input that contains no Bengali numerals`() {
        val input = "Open Chrome version 12.4"
        assertEquals(input, resolver.convertBengaliDigitsToAscii(input))
    }
}
