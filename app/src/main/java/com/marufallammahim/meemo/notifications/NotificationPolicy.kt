package com.marufallammahim.meemo.notifications

import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.domain.model.UserPreferences
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class TransientNotificationItem(
    val packageName: String,
    val appDisplayName: String,
    val senderOrTitle: String,
    val messageBody: String,
    val isContentRedactedBySystem: Boolean,
    val receivedAtMs: Long
)

/**
 * Enforces notification privacy, package allowlist, quiet hours, and duplicate suppression.
 * Notification content hashes expire after 60 seconds and raw bodies are never stored on disk.
 */
@Singleton
class NotificationPolicy @Inject constructor(
    private val timeProvider: TimeProvider
) {
    private val recentHashes = ConcurrentHashMap<String, Long>()

    fun shouldProcessNotification(
        packageName: String,
        contentSignature: String,
        prefs: UserPreferences
    ): Boolean {
        if (!prefs.notificationReadAloudEnabled) return false
        if (packageName !in prefs.allowedNotificationPackages) return false
        if (isWithinQuietHours(prefs)) return false
        if (isDuplicateWithinWindow(packageName, contentSignature)) return false
        return true
    }

    fun isWithinQuietHours(prefs: UserPreferences): Boolean {
        if (!prefs.quietHoursEnabled) return false
        val hour = timeProvider.currentHourOfDay()
        val start = prefs.quietHoursStartHour
        val end = prefs.quietHoursEndHour
        return if (start <= end) {
            hour in start until end
        } else {
            // Overnight quiet window (e.g., 23:00 to 07:00)
            hour >= start || hour < end
        }
    }

    fun isDuplicateWithinWindow(packageName: String, content: String): Boolean {
        val now = timeProvider.nowMillis()
        recentHashes.entries.removeIf { (_, timestamp) -> (now - timestamp) > DUPLICATE_WINDOW_MS }

        val hash = sha256("$packageName|$content")
        val previous = recentHashes.put(hash, now)
        return previous != null && (now - previous) <= DUPLICATE_WINDOW_MS
    }

    fun formatSpokenAnnouncement(
        item: TransientNotificationItem,
        announceSenderOnly: Boolean,
        useBengali: Boolean
    ): String {
        if (item.isContentRedactedBySystem || announceSenderOnly || item.messageBody.isBlank()) {
            return if (useBengali) {
                "${item.appDisplayName} থেকে নতুন নোটিফিকেশন এসেছে।"
            } else {
                "New notification from ${item.appDisplayName}."
            }
        }
        return if (useBengali) {
            "${item.appDisplayName}-এ ${item.senderOrTitle} লিখেছেন: ${item.messageBody}"
        } else {
            "New message on ${item.appDisplayName} from ${item.senderOrTitle}. It says: ${item.messageBody}"
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DUPLICATE_WINDOW_MS = 60_000L
    }
}
