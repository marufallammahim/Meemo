package com.meemo.core.model

data class Command(
    val rawText: String,
    val normalizedText: String,
    val intent: MeemoIntent,
    val confidence: Float,
    val source: InputSource
) {
    init {
        require(confidence in 0f..1f) { "Confidence must be between 0 and 1." }
    }
}
