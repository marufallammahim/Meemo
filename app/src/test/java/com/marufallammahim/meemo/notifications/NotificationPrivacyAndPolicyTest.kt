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
    fun `continues suppressing repeated notifications while inside sliding window`() {
        val policy = NotificationPolicy(fakeTime)

        assertFalse(policy.isDuplicateWithinWindow("com.whatsapp", "same message"))
        currentMillis += NotificationPolicy.DUPLICATE_WINDOW_MS
        assertTrue(policy.isDuplicateWithinWindow("com.whatsapp", "same message"))
        currentMillis += 1L
        assertTrue(policy.isDuplicateWithinWindow("com.whatsapp", "same message"))
        currentMillis += NotificationPolicy.DUPLICATE_WINDOW_MS + 1L
        assertFalse(policy.isDuplicateWithinWindow("com.whatsapp", "same message"))
    }

    @Test
    fun `treats signatures from the future as expired after a wall clock correction`() {
        val policy = NotificationPolicy(fakeTime)

        assertFalse(policy.isDuplicateWithinWindow("com.whatsapp", "same message"))
        currentMillis += 5_000L
        assertTrue(policy.isDuplicateWithinWindow("com.whatsapp", "same message"))

        // A backward clock correction must not leave the future timestamp suppressing
        // this notification until wall time catches up again.
        currentMillis -= 10_000L
        assertFalse(policy.isDuplicateWithinWindow("com.whatsapp", "same message"))
        assertTrue(policy.isDuplicateWithinWindow("com.whatsapp", "same message"))
    }

    @Test
    fun `bounds the duplicate cache while retaining recent signatures`() {
        val policy = NotificationPolicy(fakeTime)

        assertFalse(policy.isDuplicateWithinWindow("com.whatsapp", "message-0"))
        for (index in 1..NotificationPolicy.MAX_TRACKED_NOTIFICATIONS) {
            assertFalse(policy.isDuplicateWithinWindow("com.whatsapp", "message-$index"))
        }

        // The oldest entry is evicted at capacity, while the newest remains suppressible.
        assertFalse(policy.isDuplicateWithinWindow("com.whatsapp", "message-0"))
        assertTrue(
            policy.isDuplicateWithinWindow(
                "com.whatsapp",
                "message-${NotificationPolicy.MAX_TRACKED_NOTIFICATIONS}"
            )
        )
    }

    @Test
    fun `does not treat same notification text from different apps as duplicate`() {
        val policy = NotificationPolicy(fakeTime)

        assertFalse(policy.isDuplicateWithinWindow("com.whatsapp", "Hello"))
        assertFalse(policy.isDuplicateWithinWindow("com.telegram.messenger", "Hello"))
    }

    @Test
    fun `blocks notifications during overnight quiet hours and from non allowlisted apps`() {
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
    fun `sender only mode never includes message body in spoken announcement`() {
        val policy = NotificationPolicy(fakeTime)
        val item = TransientNotificationItem(
            packageName = "com.whatsapp",
            appDisplayName = "WhatsApp",
            senderOrTitle = "Alice",
            messageBody = "Private message text",
            isContentRedactedBySystem = false,
            receivedAtMs = currentMillis
        )

        val english = policy.formatSpokenAnnouncement(item, announceSenderOnly = true, useBengali = false)
        val bengali = policy.formatSpokenAnnouncement(item, announceSenderOnly = true, useBengali = true)

        assertEquals("New notification from WhatsApp.", english)
        assertEquals("WhatsApp থেকে নতুন নোটিফিকেশন এসেছে।", bengali)
        assertFalse(english.contains("Private message text"))
        assertFalse(bengali.contains("Private message text"))
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

    @Test
    fun `overnight quiet hours include start and exclude end boundaries`() {
        val policy = NotificationPolicy(fakeTime)
        val prefs = UserPreferences(
            notificationReadAloudEnabled = true,
            allowedNotificationPackages = setOf("com.whatsapp"),
            quietHoursEnabled = true,
            quietHoursStartHour = 23,
            quietHoursEndHour = 7
        )

        currentHour = 22
        assertFalse(policy.isWithinQuietHours(prefs))
        currentHour = 23
        assertTrue(policy.isWithinQuietHours(prefs))
        currentHour = 6
        assertTrue(policy.isWithinQuietHours(prefs))
        currentHour = 7
        assertFalse(policy.isWithinQuietHours(prefs))
    }

    @Test
    fun `formats full notification announcements in both languages`() {
        val policy = NotificationPolicy(fakeTime)
        val item = TransientNotificationItem(
            packageName = "com.whatsapp",
            appDisplayName = "WhatsApp",
            senderOrTitle = "Alice",
            messageBody = "See you soon",
            isContentRedactedBySystem = false,
            receivedAtMs = currentMillis
        )

        assertEquals(
            "New message on WhatsApp from Alice. It says: See you soon",
            policy.formatSpokenAnnouncement(item, announceSenderOnly = false, useBengali = false)
        )
        assertEquals(
            "WhatsApp-এ Alice লিখেছেন: See you soon",
            policy.formatSpokenAnnouncement(item, announceSenderOnly = false, useBengali = true)
        )
    }

    @Test
    fun `blank notification body falls back to app level announcement`() {
        val policy = NotificationPolicy(fakeTime)
        val item = TransientNotificationItem(
            packageName = "com.whatsapp",
            appDisplayName = "WhatsApp",
            senderOrTitle = "Alice",
            messageBody = "   ",
            isContentRedactedBySystem = false,
            receivedAtMs = currentMillis
        )

        assertEquals(
            "New notification from WhatsApp.",
            policy.formatSpokenAnnouncement(item, announceSenderOnly = false, useBengali = false)
        )
    }

    @Test
    fun `daytime quiet hours include start and exclude end boundaries`() {
        val policy = NotificationPolicy(fakeTime)
        val prefs = UserPreferences(
            notificationReadAloudEnabled = true,
            allowedNotificationPackages = setOf("com.whatsapp"),
            quietHoursEnabled = true,
            quietHoursStartHour = 9,
            quietHoursEndHour = 17
        )

        currentHour = 8
        assertFalse(policy.isWithinQuietHours(prefs))
        currentHour = 9
        assertTrue(policy.isWithinQuietHours(prefs))
        currentHour = 16
        assertTrue(policy.isWithinQuietHours(prefs))
        currentHour = 17
        assertFalse(policy.isWithinQuietHours(prefs))
    }
}
