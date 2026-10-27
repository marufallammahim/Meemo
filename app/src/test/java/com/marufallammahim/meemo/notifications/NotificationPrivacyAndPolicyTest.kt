package com.marufallammahim.meemo.notifications

import com.marufallammahim.meemo.core.logging.SafeLogger
import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPrivacyAndPolicyTest {

    private var currentMillis = 1_700_000_000_000L
    private var currentHour = 14

    private val fakeTime = object : TimeProvider {
        override fun nowMillis(): Long = currentMillis
        override fun currentHourOfDay(): Int = currentHour
    }

    @Test
    fun `suppresses duplicate notifications within 60 second window`() {
        val policy = NotificationPolicy(fakeTime)
        val prefs = UserPreferences(
            notificationReadAloudEnabled = true,
            allowedNotificationPackages = setOf("com.whatsapp"),
            quietHoursEnabled = false
        )

        assertTrue(policy.shouldProcessNotification("com.whatsapp", "Alice:Are you coming?", prefs))
        // Immediate duplicate must be suppressed
        assertFalse(policy.shouldProcessNotification("com.whatsapp", "Alice:Are you coming?", prefs))

        // Advance past 60s window
        currentMillis += 61_000L
        assertTrue(policy.shouldProcessNotification("com.whatsapp", "Alice:Are you coming?", prefs))
    }

    @Test
    fun `blocks notifications during overnight quiet hours and from non-allowlisted apps`() {
        val policy = NotificationPolicy(fakeTime)
        val prefs = UserPreferences(
            notificationReadAloudEnabled = true,
            allowedNotificationPackages = setOf("com.whatsapp"),
            quietHoursEnabled = true,
            quietHoursStartHour = 23,
            quietHoursEndHour = 7
        )

        currentHour = 2 // 2:00 AM inside quiet window
        assertFalse(policy.shouldProcessNotification("com.whatsapp", "Hello", prefs))

        currentHour = 12 // Noon outside quiet window, but unlisted package
        assertFalse(policy.shouldProcessNotification("com.unknown.game", "Hello", prefs))
    }

    @Test
    fun `redacts lockscreen secret notifications and SafeLogger sensitive tokens`() {
        val policy = NotificationPolicy(fakeTime)
        val secretItem = TransientNotificationItem(
            packageName = "com.whatsapp",
            appDisplayName = "WhatsApp",
            senderOrTitle = "Alice",
            messageBody = "Secret OTP 123456",
            isContentRedactedBySystem = true,
            receivedAtMs = currentMillis
        )
        val spoken = policy.formatSpokenAnnouncement(secretItem, announceSenderOnly = false, useBengali = false)
        assertEquals("New notification from WhatsApp.", spoken)
        assertFalse(spoken.contains("123456"))

        val redactedLog = SafeLogger.redactSensitiveContent(
            "Header Authorization: Bearer eyJhbGciOiJIUzI1NiJ9 api_key=sk-1234567890abcdef notification_body=[Private text]"
        )
        assertFalse(redactedLog.contains("eyJhbGciOiJIUzI1NiJ9"))
        assertFalse(redactedLog.contains("sk-1234567890abcdef"))
        assertFalse(redactedLog.contains("Private text"))
        assertTrue(redactedLog.contains("[REDACTED_TOKEN]"))
    }
}
