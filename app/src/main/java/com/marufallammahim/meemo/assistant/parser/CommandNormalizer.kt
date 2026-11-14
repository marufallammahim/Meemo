package com.marufallammahim.meemo.assistant.parser

import com.marufallammahim.meemo.assistant.command.CommandInputSource
import com.marufallammahim.meemo.assistant.command.NormalizedCommand
import com.marufallammahim.meemo.core.utilities.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Normalizes raw voice or text input without destroying application names or numbers.
 * Handles wake-phrase stripping ("Hello Meemo", "হ্যালো মিমো"), Bengali numeral conversion,
 * and whitespace/punctuation cleanup.
 */
@Singleton
class CommandNormalizer @Inject constructor(
    private val languageResolver: LanguageResolver,
    private val timeProvider: TimeProvider
) {
    fun normalize(
        rawInput: String,
        source: CommandInputSource = CommandInputSource.TEXT
    ): NormalizedCommand {
        val trimmed = rawInput.trim()
        val detectedLang = languageResolver.detectLanguage(trimmed)
        val asciiDigits = languageResolver.convertBengaliDigitsToAscii(trimmed)

        var working = asciiDigits
        var wakeDetected = false

        for (regex in WAKE_PHRASE_PATTERNS) {
            val match = regex.find(working)
            if (match != null && match.range.first == 0) {
                wakeDetected = true
                working = working.removeRange(match.range).trim()
                break
            }
        }

        // Remove polite trailing/leading punctuation while preserving dots/hyphens inside package/app names
        val cleaned = working
            .replace(Regex("^[\\s,.:;!?।\\-]+"), "")
            .replace(Regex("[\\s,.:;!?।]+$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return NormalizedCommand(
            rawText = rawInput,
            normalizedText = cleaned,
            wakePhraseDetected = wakeDetected,
            detectedLanguage = detectedLang,
            source = source,
            timestampMs = timeProvider.nowMillis()
        )
    }

    companion object {
        private val WAKE_PHRASE_PATTERNS = listOf(
            Regex("(?i)^(hello|hey|hi|ok|okay)\\s+meemo[,.:!\\s]*"),
            Regex("(?i)^meemo[,.:!\\s]+"),
            Regex("^(হ্যালো|হাই|ওকে|এই)\\s*মিমো[,।:!\\s]*"),
            Regex("^মিমো[,।:!\\s]+")
        )
    }
}
