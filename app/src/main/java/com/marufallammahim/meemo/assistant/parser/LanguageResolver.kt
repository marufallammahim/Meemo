package com.marufallammahim.meemo.assistant.parser

import com.marufallammahim.meemo.assistant.command.DetectedLanguage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects whether a command is written/spoken in English, Bengali script (বাংলা),
 * or transliterated Banglish, and normalizes Bengali numerals (০-৯) to ASCII (0-9).
 */
@Singleton
class LanguageResolver @Inject constructor() {

    fun detectLanguage(input: String): DetectedLanguage {
        var hasBengaliScript = false
        var hasLatinScript = false
        for (ch in input) {
            when (ch.code) {
                in 0x0980..0x09FF -> hasBengaliScript = true
                in 0x0041..0x005A, in 0x0061..0x007A -> hasLatinScript = true
            }
        }
        if (hasBengaliScript && hasLatinScript) return DetectedLanguage.MIXED_BANGLISH
        if (hasBengaliScript) return DetectedLanguage.BENGALI

        val lower = input.lowercase()
        val hasBanglishKeywords = BANGLISH_MARKERS.any { marker ->
            Regex("\\b$marker\\b").containsMatchIn(lower)
        }
        return if (hasBanglishKeywords) DetectedLanguage.MIXED_BANGLISH else DetectedLanguage.ENGLISH
    }

    fun convertBengaliDigitsToAscii(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val mapped = when (ch) {
                '০' -> '0'
                '১' -> '1'
                '২' -> '2'
                '৩' -> '3'
                '৪' -> '4'
                '৫' -> '5'
                '৬' -> '6'
                '৭' -> '7'
                '৮' -> '8'
                '৯' -> '9'
                else -> ch
            }
            sb.append(mapped)
        }
        return sb.toString()
    }

    companion object {
        private val BANGLISH_MARKERS = listOf(
            "kholo", "khulun", "chalu", "bondho", "koro", "korun",
            "koto", "baki", "komao", "barao", "shono", "bolo"
        )
    }
}
