package com.marufallammahim.meemo.assistant.command

enum class DetectedLanguage {
    ENGLISH,
    BENGALI,
    MIXED_BANGLISH
}

enum class CommandInputSource {
    TEXT,
    VOICE_MANUAL,
    VOICE_WAKE_WORD,
    ALIAS_EXPANSION,
    AUTOMATION_ROUTINE
}

data class NormalizedCommand(
    val rawText: String,
    val normalizedText: String,
    val wakePhraseDetected: Boolean,
    val detectedLanguage: DetectedLanguage,
    val source: CommandInputSource,
    val timestampMs: Long
)
