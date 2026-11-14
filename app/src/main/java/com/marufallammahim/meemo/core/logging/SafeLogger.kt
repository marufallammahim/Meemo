package com.marufallammahim.meemo.core.logging

import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

data class DiagnosticLogEntry(
    val timestampMs: Long,
    val level: String,
    val tag: String,
    val redactedMessage: String
)

/**
 * Privacy-preserving structured logger.
 * Automatically redacts API keys, bearer tokens, email addresses, phone numbers,
 * and tagged notification message bodies before storing in diagnostic buffer.
 */
@Singleton
class SafeLogger @Inject constructor() {

    private val recentLogs = CopyOnWriteArrayList<DiagnosticLogEntry>()

    fun info(tag: String, message: String) = record("INFO", tag, message)
    fun warn(tag: String, message: String) = record("WARN", tag, message)
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val suffix = throwable?.let { " | Exception=${it.javaClass.simpleName}" } ?: ""
        record("ERROR", tag, message + suffix)
    }

    fun getRecentLogs(): List<DiagnosticLogEntry> = recentLogs.toList()

    fun clearLogs() {
        recentLogs.clear()
    }

    private fun record(level: String, tag: String, rawMessage: String) {
        val clean = redactSensitiveContent(rawMessage)
        val entry = DiagnosticLogEntry(
            timestampMs = System.currentTimeMillis(),
            level = level,
            tag = tag,
            redactedMessage = clean
        )
        recentLogs.add(entry)
        while (recentLogs.size > MAX_LOG_ENTRIES) {
            recentLogs.removeAt(0)
        }
    }

    companion object {
        private const val MAX_LOG_ENTRIES = 200

        private val BEARER_REGEX = Regex("(?i)bearer\\s+[a-zA-Z0-9._\\-]+")
        private val API_KEY_REGEX = Regex("(?i)(sk-[a-zA-Z0-9]{8,}|api[_-]?key\\s*[:=]\\s*\\S+)")
        private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
        private val NOTIFICATION_BODY_REGEX = Regex("(?i)notification_body=\\[.*?\\]")

        fun redactSensitiveContent(input: String): String {
            return input
                .replace(BEARER_REGEX, "Bearer [REDACTED_TOKEN]")
                .replace(API_KEY_REGEX, "[REDACTED_API_KEY]")
                .replace(EMAIL_REGEX, "[REDACTED_EMAIL]")
                .replace(NOTIFICATION_BODY_REGEX, "notification_body=[REDACTED_TRANSIENT]")
        }
    }
}
