package com.meemo.nlu

import java.util.Locale

class InputNormalizer {
    fun normalize(input: String): String {
        val lower = input.lowercase(Locale.ROOT)
        val withoutWakePhrase = lower
            .replace(Regex("\\b(hey\\s+meemo|hello\\s+meemo|meemo)\\b"), " ")
            .replace(Regex("\\b(please|can you|could you|would you)\\b"), " ")
        return withoutWakePhrase
            .replace(Regex("[^\\p{L}\\p{N}%]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
