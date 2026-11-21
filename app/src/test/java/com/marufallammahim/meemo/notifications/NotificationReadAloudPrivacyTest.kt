package com.marufallammahim.meemo.notifications

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.domain.model.UserPreferences
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.tts.TtsController
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationReadAloudPrivacyTest {
    private val fixedTime = object : TimeProvider {
        override fun nowMillis(): Long = 1_700_000_000_000L
        override fun currentHourOfDay(): Int = 14
    }

    @Test
    fun `sender only mode does not retain title or body after preferences change`() = runTest {
        val preferences = MutableStateFlow(
            UserPreferences(
                notificationReadAloudEnabled = true,
                notificationAnnounceSenderOnly = true,
                allowedNotificationPackages = setOf("com.whatsapp"),
                ttsEnabled = false
            )
        )
        val repository = mockk<MeemoRepository>()
        every { repository.userPreferencesFlow } returns preferences
        val readAloud = NotificationReadAloud(
            policy = NotificationPolicy(fixedTime),
            ttsController = mockk<TtsController>(relaxed = true),
            repository = repository
        )
        val item = TransientNotificationItem(
            packageName = "com.whatsapp",
            appDisplayName = "WhatsApp",
            senderOrTitle = "Alice",
            messageBody = "Confidential message preview",
            isContentRedactedBySystem = false,
            receivedAtMs = fixedTime.nowMillis()
        )

        readAloud.handleIncomingNotification(item)
        preferences.value = preferences.value.copy(notificationAnnounceSenderOnly = false)

        val result = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        assertTrue(result.summaryEn.contains("WhatsApp"))
        assertFalse(result.summaryEn.contains("Alice"))
        assertFalse(result.summaryEn.contains("Confidential message preview"))
        assertFalse(result.summaryBn.contains("Confidential message preview"))
    }

    @Test
    fun `system redacted items discard supplied preview text before queueing`() = runTest {
        val preferences = MutableStateFlow(
            UserPreferences(
                notificationReadAloudEnabled = true,
                allowedNotificationPackages = setOf("com.whatsapp"),
                ttsEnabled = false
            )
        )
        val repository = mockk<MeemoRepository>()
        every { repository.userPreferencesFlow } returns preferences
        val readAloud = NotificationReadAloud(
            policy = NotificationPolicy(fixedTime),
            ttsController = mockk<TtsController>(relaxed = true),
            repository = repository
        )
        val item = TransientNotificationItem(
            packageName = "com.whatsapp",
            appDisplayName = "WhatsApp",
            senderOrTitle = "Sensitive title",
            messageBody = "Secret OTP 123456",
            isContentRedactedBySystem = true,
            receivedAtMs = fixedTime.nowMillis()
        )

        readAloud.handleIncomingNotification(item)

        val result = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        assertFalse(result.summaryEn.contains("Sensitive title"))
        assertFalse(result.summaryEn.contains("Secret OTP 123456"))
    }
}
