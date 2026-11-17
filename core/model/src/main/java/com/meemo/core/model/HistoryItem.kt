package com.meemo.core.model

data class HistoryItem(
    val id: String,
    val rawText: String,
    val normalizedText: String,
    val intentName: String,
    val outcomeName: String,
    val createdAtEpochMillis: Long
)
