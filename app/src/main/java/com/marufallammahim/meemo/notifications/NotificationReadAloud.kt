package com.marufallammahim.meemo.notifications

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.tts.TtsController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a bounded transient in-memory queue (max 15 items, max age 15 minutes)
 * for notification read-aloud. Never persists notification text to Room or disk.
 */
@Singleton
class NotificationReadAloud @Inject constructor(
    private val policy: NotificationPolicy,
    private val ttsController: TtsController,
    private val repository: MeemoRepository,
    private val timeProvider: TimeProvider
) {
    private val transientQueue = ArrayDeque<TransientNotificationItem>()
    private val policyMutex = Mutex()

    /**
     * Serializes preference changes with notification submission. A callback that
     * started before an opt-out must finish before the opt-out cancels its speech.
     */
    suspend fun setTtsEnabled(enabled: Boolean) {
        policyMutex.withLock {
            repository.setTtsEnabled(enabled)
            if (!enabled) ttsController.stopSpeaking()
        }
    }

    suspend fun setNotificationReadAloudEnabled(enabled: Boolean) {
        policyMutex.withLock {
            repository.setNotificationReadAloud(enabled)
            if (!enabled) {
                clearTransientQueue()
                ttsController.stopNotificationSpeech()
            }
        }
    }

    suspend fun setNotificationSenderOnly(senderOnly: Boolean) {
        policyMutex.withLock {
            repository.setNotificationAnnounceSenderOnly(senderOnly)
            if (senderOnly) {
                synchronized(this) {
                    val redactedItems = transientQueue.map {
                        it.copy(senderOrTitle = "", messageBody = "")
                    }
                    transientQueue.clear()
                    transientQueue.addAll(redactedItems)
                }
                ttsController.stopNotificationSpeech()
            }
        }
    }

    suspend fun handleIncomingNotification(item: TransientNotificationItem) {
        policyMutex.withLock {
            val prefs = repository.userPreferencesFlow.first()
            val nowMillis = timeProvider.nowMillis()
            if (!prefs.notificationReadAloudEnabled) {
                clearTransientQueue()
                ttsController.stopNotificationSpeech()
                return@withLock
            }
            if (!isRecent(item.receivedAtMs, nowMillis)) return@withLock

            val signature = "${item.senderOrTitle}:${item.messageBody}"
            if (!policy.shouldProcessNotification(item.packageName, signature, prefs)) {
                return@withLock
            }

            // Sender-only and system-redacted notifications must not retain preview text.
            val queuedItem = if (prefs.notificationAnnounceSenderOnly || item.isContentRedactedBySystem) {
                item.copy(senderOrTitle = "", messageBody = "")
            } else {
                item
            }
            enqueueTransient(queuedItem)

            if (prefs.ttsEnabled) {
                val useBengali = prefs.languageMode == AppLanguageMode.BENGALI
                val announcement = policy.formatSpokenAnnouncement(
                    item = queuedItem,
                    announceSenderOnly = prefs.notificationAnnounceSenderOnly,
                    useBengali = useBengali
                )
                ttsController.enqueueNotificationSpeech(
                    text = announcement,
                    languageTag = if (useBengali) "bn-BD" else "en-US",
                    speechRate = prefs.ttsSpeechRate,
                    expiresAtMillis = item.receivedAtMs + MAX_TRANSIENT_AGE_MS
                )
            }
        }
    }

    suspend fun readQueuedNotificationsSummary(): ActionResult = policyMutex.withLock {
        val prefs = repository.userPreferencesFlow.first()
        var shouldCancelNotificationSpeech = false
        val snapshot = synchronized(this) {
            pruneExpiredItems(timeProvider.nowMillis())
            if (!prefs.notificationReadAloudEnabled) {
                transientQueue.clear()
                shouldCancelNotificationSpeech = true
                emptyList()
            } else {
                val sizeBeforeAllowlist = transientQueue.size
                transientQueue.removeIf { it.packageName !in prefs.allowedNotificationPackages }
                if (transientQueue.size != sizeBeforeAllowlist) {
                    shouldCancelNotificationSpeech = true
                }

                if (prefs.notificationAnnounceSenderOnly) {
                    val containsPreview = transientQueue.any {
                        it.senderOrTitle.isNotBlank() || it.messageBody.isNotBlank()
                    }
                    if (containsPreview) shouldCancelNotificationSpeech = true
                    val redactedItems = transientQueue.map {
                        it.copy(senderOrTitle = "", messageBody = "")
                    }
                    transientQueue.clear()
                    transientQueue.addAll(redactedItems)
                }
                transientQueue.toList()
            }
        }

        if (shouldCancelNotificationSpeech) ttsController.stopNotificationSpeech()

        if (snapshot.isEmpty()) {
            return@withLock ActionResult.Success(
                summaryEn = "No recent unread notifications in the active session queue.",
                summaryBn = "এই মুহূর্তে কোনো নতুন নোটিফিকেশন জমা নেই।",
                verifiedBySystem = true
            )
        }
        val latest = snapshot.last()
        val spokenEn = policy.formatSpokenAnnouncement(
            latest,
            prefs.notificationAnnounceSenderOnly,
            useBengali = false
        )
        val spokenBn = policy.formatSpokenAnnouncement(
            latest,
            prefs.notificationAnnounceSenderOnly,
            useBengali = true
        )
        ActionResult.Success(
            summaryEn = "Latest of ${snapshot.size} notification(s): $spokenEn",
            summaryBn = "মোট ${snapshot.size}টি নোটিফিকেশনের সর্বশেষটি: $spokenBn",
            verifiedBySystem = true
        )
    }

    private fun enqueueTransient(item: TransientNotificationItem) {
        synchronized(this) {
            val nowMillis = timeProvider.nowMillis()
            pruneExpiredItems(nowMillis)
            if (!isRecent(item.receivedAtMs, nowMillis)) return
            while (transientQueue.size >= MAX_TRANSIENT_ITEMS) {
                transientQueue.pollFirst()
            }
            transientQueue.addLast(item)
        }
    }

    private fun clearTransientQueue() {
        synchronized(this) {
            transientQueue.clear()
        }
    }

    private fun pruneExpiredItems(nowMillis: Long) {
        val iterator = transientQueue.iterator()
        while (iterator.hasNext()) {
            if (!isRecent(iterator.next().receivedAtMs, nowMillis)) {
                iterator.remove()
            }
        }
    }

    private fun isRecent(receivedAtMs: Long, nowMillis: Long): Boolean =
        receivedAtMs <= nowMillis && receivedAtMs >= nowMillis - MAX_TRANSIENT_AGE_MS

    companion object {
        private const val MAX_TRANSIENT_ITEMS = 15
        const val MAX_TRANSIENT_AGE_MS = 15 * 60 * 1000L
    }
}
