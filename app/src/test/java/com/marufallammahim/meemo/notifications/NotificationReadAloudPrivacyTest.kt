package com.marufallammahim.meemo.notifications

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.domain.model.UserPreferences
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.tts.TtsController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationReadAloudPrivacyTest {
    private var currentMillis = 1_700_000_000_000L

    private val fixedTime = object : TimeProvider {
        override fun nowMillis(): Long = currentMillis
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
        val readAloud = createReadAloud(preferences)

        readAloud.handleIncomingNotification(sampleNotification())
        preferences.value = preferences.value.copy(notificationAnnounceSenderOnly = false)

        val result = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        assertTrue(result.summaryEn.contains("WhatsApp"))
        assertFalse(result.summaryEn.contains("Alice"))
        assertFalse(result.summaryEn.contains("Confidential message preview"))
        assertFalse(result.summaryBn.contains("Confidential message preview"))
    }

    @Test
    fun `system redacted items discard supplied preview text before queueing`() = runTest {
        val preferences = enabledPreferences()
        val readAloud = createReadAloud(preferences)

        readAloud.handleIncomingNotification(
            sampleNotification(
                senderOrTitle = "Sensitive title",
                messageBody = "Secret OTP 123456",
                isContentRedactedBySystem = true
            )
        )

        val result = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        assertFalse(result.summaryEn.contains("Sensitive title"))
        assertFalse(result.summaryEn.contains("Secret OTP 123456"))
    }

    @Test
    fun `expired notification previews are removed before they can be read`() = runTest {
        val preferences = enabledPreferences()
        val readAloud = createReadAloud(preferences)

        readAloud.handleIncomingNotification(sampleNotification())
        currentMillis += NotificationReadAloud.MAX_TRANSIENT_AGE_MS + 1L

        val result = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        assertTrue(result.summaryEn.contains("No recent unread notifications"))
        assertFalse(result.summaryEn.contains("Confidential message preview"))
    }

    @Test
    fun `disabling notification read aloud clears the transient queue`() = runTest {
        val preferences = enabledPreferences()
        val readAloud = createReadAloud(preferences)
        readAloud.handleIncomingNotification(sampleNotification())

        preferences.value = preferences.value.copy(notificationReadAloudEnabled = false)
        val disabledResult = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        assertTrue(disabledResult.summaryEn.contains("No recent unread notifications"))

        preferences.value = preferences.value.copy(notificationReadAloudEnabled = true)
        val reenabledResult = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        assertTrue(reenabledResult.summaryEn.contains("No recent unread notifications"))
    }

    @Test
    fun `sender only preference change purges previews already queued in memory`() = runTest {
        val preferences = enabledPreferences()
        val ttsController = mockk<TtsController>(relaxed = true)
        val readAloud = createReadAloud(preferences, ttsController)
        readAloud.handleIncomingNotification(sampleNotification())

        preferences.value = preferences.value.copy(notificationAnnounceSenderOnly = true)
        val redactedResult = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        verify(exactly = 1) { ttsController.stopSpeaking() }
        assertFalse(redactedResult.summaryEn.contains("Confidential message preview"))

        preferences.value = preferences.value.copy(notificationAnnounceSenderOnly = false)
        val laterResult = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        assertFalse(laterResult.summaryEn.contains("Confidential message preview"))
    }

    @Test
    fun `removing an app from the allowlist removes its queued notifications`() = runTest {
        val preferences = enabledPreferences()
        val ttsController = mockk<TtsController>(relaxed = true)
        val readAloud = createReadAloud(preferences, ttsController)
        readAloud.handleIncomingNotification(sampleNotification())

        preferences.value = preferences.value.copy(allowedNotificationPackages = emptySet())
        val result = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        verify(exactly = 1) { ttsController.stopSpeaking() }

        assertTrue(result.summaryEn.contains("No recent unread notifications"))
        assertFalse(result.summaryEn.contains("WhatsApp"))
    }

    private fun createReadAloud(
        preferences: MutableStateFlow<UserPreferences>,
        ttsController: TtsController = mockk(relaxed = true)
    ): NotificationReadAloud {
        val repository = mockk<MeemoRepository>()
        every { repository.userPreferencesFlow } returns preferences
        return NotificationReadAloud(
            policy = NotificationPolicy(fixedTime),
            ttsController = ttsController,
            repository = repository,
            timeProvider = fixedTime
        )
    }

    private fun enabledPreferences() = MutableStateFlow(
        UserPreferences(
            notificationReadAloudEnabled = true,
            allowedNotificationPackages = setOf("com.whatsapp"),
            ttsEnabled = false
        )
    )

    private fun sampleNotification(
        senderOrTitle: String = "Alice",
        messageBody: String = "Confidential message preview",
        isContentRedactedBySystem: Boolean = false
    ) = TransientNotificationItem(
        packageName = "com.whatsapp",
        appDisplayName = "WhatsApp",
        senderOrTitle = senderOrTitle,
        messageBody = messageBody,
        isContentRedactedBySystem = isContentRedactedBySystem,
        receivedAtMs = fixedTime.nowMillis()
    )
}
