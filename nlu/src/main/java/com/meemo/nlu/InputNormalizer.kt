package com.meemo.nlu

import java.util.Locale

class InputNormalizer {
    fun normalize(input: String): String {
        // Trim before anchored wake-phrase matching so leading ASR whitespace
        // cannot prevent Bengali greetings from being removed.
        val lower = input.trim().lowercase(Locale.ROOT)
        val withoutWakePhrase = lower
            .replace(Regex("\\b(hey\\s+meemo|hello\\s+meemo|meemo)\\b"), " ")
            .replace(Regex("^(?:হ্যালো|হাই|ওকে)[\\s,।:!]*মিমো[\\s,।:!]*"), " ")
            .replace(Regex("^মিমো[\\s,।:!]*"), " ")
            .replace(Regex("\\b(please|can you|could you|would you)\\b"), " ")
            .replace(Regex("^\\s*(?:দয়া করে|অনুগ্রহ করে)\\s*"), " ")
        return withoutWakePhrase
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}%]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}

internal fun String.toAsciiDigits(): String = buildString(length) {
    for (character in this@toAsciiDigits) {
        val digit = character.digitToIntOrNull()
        append(if (digit == null) character else ('0'.code + digit).toChar())
    }
}
