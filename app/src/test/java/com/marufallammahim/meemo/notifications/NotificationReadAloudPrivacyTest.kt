package com.marufallammahim.meemo.notifications

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.domain.model.UserPreferences
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.tts.TtsController
import io.mockk.any
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
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
    fun `disabling read aloud clears the queue and cancels only notification speech`() = runTest {
        val preferences = enabledPreferences()
        val ttsController = mockk<TtsController>(relaxed = true)
        val readAloud = createReadAloud(preferences, ttsController)
        readAloud.handleIncomingNotification(sampleNotification())

        readAloud.setNotificationReadAloudEnabled(false)
        preferences.value = preferences.value.copy(notificationReadAloudEnabled = false)
        val disabledResult = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        assertTrue(disabledResult.summaryEn.contains("No recent unread notifications"))
        verify(atLeast = 1) { ttsController.stopNotificationSpeech() }
        verify(exactly = 0) { ttsController.stopSpeaking() }
    }

    @Test
    fun `disabled read aloud rejects callbacks and cancels notification speech`() = runTest {
        val preferences = enabledPreferences()
        preferences.value = preferences.value.copy(notificationReadAloudEnabled = false)
        val ttsController = mockk<TtsController>(relaxed = true)
        val readAloud = createReadAloud(preferences, ttsController)

        readAloud.handleIncomingNotification(sampleNotification())

        verify(exactly = 1) { ttsController.stopNotificationSpeech() }
        verify(exactly = 0) {
            ttsController.enqueueNotificationSpeech(any(), any(), any(), any())
        }
    }

    @Test
    fun `sender only preference change purges previews and cancels notification speech`() = runTest {
        val preferences = enabledPreferences()
        val ttsController = mockk<TtsController>(relaxed = true)
        val readAloud = createReadAloud(preferences, ttsController)
        readAloud.handleIncomingNotification(sampleNotification())

        readAloud.setNotificationSenderOnly(true)
        preferences.value = preferences.value.copy(notificationAnnounceSenderOnly = true)
        val redactedResult = readAloud.readQueuedNotificationsSummary() as ActionResult.Success
        assertFalse(redactedResult.summaryEn.contains("Confidential message preview"))
        verify(atLeast = 1) { ttsController.stopNotificationSpeech() }
        verify(exactly = 0) { ttsController.stopSpeaking() }
    }

    @Test
    fun `removing an app from the allowlist removes its queued notifications`() = runTest {
        val preferences = enabledPreferences()
        val ttsController = mockk<TtsController>(relaxed = true)
        val readAloud = createReadAloud(preferences, ttsController)
        readAloud.handleIncomingNotification(sampleNotification())

        preferences.value = preferences.value.copy(allowedNotificationPackages = emptySet())
        val result = readAloud.readQueuedNotificationsSummary() as ActionResult.Success

        assertTrue(result.summaryEn.contains("No recent unread notifications"))
        assertFalse(result.summaryEn.contains("WhatsApp"))
        verify { ttsController.stopNotificationSpeech() }
        verify(exactly = 0) { ttsController.stopSpeaking() }
    }

    @Test
    fun `opt out waits for an in flight notification before cancelling its speech`() = runTest {
        val initialPreferences = UserPreferences(
            notificationReadAloudEnabled = true,
            allowedNotificationPackages = setOf("com.whatsapp"),
            ttsEnabled = true
        )
        val flowEntered = CompletableDeferred<Unit>()
        val releasePreferences = CompletableDeferred<Unit>()
        val delayedPreferences: Flow<UserPreferences> = flow {
            flowEntered.complete(Unit)
            releasePreferences.await()
            emit(initialPreferences)
        }
        val repository = mockk<MeemoRepository>(relaxed = true)
        every { repository.userPreferencesFlow } returns delayedPreferences
        val ttsController = mockk<TtsController>(relaxed = true)
        val readAloud = createReadAloud(delayedPreferences, ttsController, repository)

        val incoming = launch { readAloud.handleIncomingNotification(sampleNotification()) }
        flowEntered.await()
        val optOut = launch { readAloud.setNotificationReadAloudEnabled(false) }
        runCurrent()
        releasePreferences.complete(Unit)
        incoming.join()
        optOut.join()

        coVerify { repository.setNotificationReadAloud(false) }
        verifyOrder {
            ttsController.enqueueNotificationSpeech(any(), any(), any(), any())
            ttsController.stopNotificationSpeech()
        }
    }

    private fun createReadAloud(
        preferences: Flow<UserPreferences>,
        ttsController: TtsController = mockk(relaxed = true),
        repository: MeemoRepository = mockk(relaxed = true)
    ): NotificationReadAloud {
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
