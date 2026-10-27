package com.marufallammahim.meemo.assistant.context

import com.marufallammahim.meemo.core.utilities.TimeProvider
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages active conversation session ID and prevents duplicate rapid-fire command submissions.
 */
@Singleton
class ConversationContextManager @Inject constructor(
    private val timeProvider: TimeProvider
) {
    private var activeSessionId: String = UUID.randomUUID().toString()
    private var lastNormalizedCommand: String = ""
    private var lastCommandTimestampMs: Long = 0L

    fun currentSessionId(): String = activeSessionId

    fun startNewSession(): String {
        activeSessionId = UUID.randomUUID().toString()
        lastNormalizedCommand = ""
        lastCommandTimestampMs = 0L
        return activeSessionId
    }

    fun isDuplicateSubmission(normalizedText: String): Boolean {
        val now = timeProvider.nowMillis()
        val clean = normalizedText.trim().lowercase()
        if (clean.isNotEmpty() &&
            clean == lastNormalizedCommand &&
            (now - lastCommandTimestampMs) in 0..DUPLICATE_DEBOUNCE_WINDOW_MS
        ) {
            return true
        }
        lastNormalizedCommand = clean
        lastCommandTimestampMs = now
        return false
    }

    companion object {
        const val DUPLICATE_DEBOUNCE_WINDOW_MS = 1_200L
    }
}
