package com.meemo.nlu

class SynonymTable {
    private val phraseReplacements = listOf(
        Regex("\\bswitch\\s+on\\b") to "on",
        Regex("\\bturn\\s+on\\b") to "on",
        Regex("\\bturn\\s+off\\b") to "off",
        Regex("\\bswitch\\s+off\\b") to "off",
        Regex("\\bwi\\s+fi\\b") to "wifi"
    )
    private val words = mapOf(
        "torch" to "flashlight", "flash" to "flashlight", "light" to "flashlight",
        "enable" to "on", "start" to "open", "launch" to "open", "run" to "open",
        "wireless" to "wifi"
    )
    fun canonicalize(input: String): String {
        var result = input
        phraseReplacements.forEach { (pattern, replacement) -> result = pattern.replace(result, replacement) }
        return result.split(Regex("\\s+")).joinToString(" ") { words[it] ?: it }
            .replace(Regex("\\s+"), " ").trim()
    }
}
