package com.marufallammahim.meemo.notifications

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.tts.TtsController
import kotlinx.coroutines.flow.first
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

    @Synchronized
    fun enqueueTransient(item: TransientNotificationItem) {
        val nowMillis = timeProvider.nowMillis()
        val sizeBeforePruning = transientQueue.size
        pruneExpiredItems(nowMillis)
        var invalidatedSpeech = transientQueue.size != sizeBeforePruning

        if (!isRecent(item.receivedAtMs, nowMillis)) {
            if (invalidatedSpeech) ttsController.stopSpeaking()
            return
        }

        if (transientQueue.size >= MAX_TRANSIENT_ITEMS) {
            transientQueue.pollFirst()
            invalidatedSpeech = true
        }
        transientQueue.addLast(item)

        // The platform queue cannot remove individual notification utterances.
        // Stop stale speech while holding the queue lock so a newer enqueue is not cancelled.
        if (invalidatedSpeech) ttsController.stopSpeaking()
    }

    @Synchronized
    fun clearTransientQueue() {
        transientQueue.clear()
    }

    suspend fun handleIncomingNotification(item: TransientNotificationItem) {
        val prefs = repository.userPreferencesFlow.first()
        if (!prefs.notificationReadAloudEnabled) {
            clearTransientQueue()
            // The platform TTS queue is shared with assistant responses. Stop it so a
            // notification preview queued before this preference change cannot leak.
            ttsController.stopSpeaking()
            return
        }

        val signature = "${item.senderOrTitle}:${item.messageBody}"
        if (!policy.shouldProcessNotification(item.packageName, signature, prefs)) {
            return
        }

        // Sender-only and system-redacted notifications must not retain preview text in memory.
        // This also prevents a later preference change from exposing an earlier queued message.
        val queuedItem = if (prefs.notificationAnnounceSenderOnly || item.isContentRedactedBySystem) {
            item.copy(senderOrTitle = "", messageBody = "")
        } else {
            item
        }
        enqueueTransient(queuedItem)

        val useBengali = prefs.languageMode == AppLanguageMode.BENGALI
        val announcement = policy.formatSpokenAnnouncement(
            item = queuedItem,
            announceSenderOnly = prefs.notificationAnnounceSenderOnly,
            useBengali = useBengali
        )
        if (prefs.ttsEnabled) {
            ttsController.enqueueNotificationSpeech(
                text = announcement,
                languageTag = if (useBengali) "bn-BD" else "en-US",
                speechRate = prefs.ttsSpeechRate
            )
        }
    }

    suspend fun readQueuedNotificationsSummary(): ActionResult {
        val prefs = repository.userPreferencesFlow.first()
        var shouldStopSpeech = false
        val snapshot = synchronized(this) {
            val sizeBeforePruning = transientQueue.size
            pruneExpiredItems(timeProvider.nowMillis())
            if (transientQueue.size != sizeBeforePruning) shouldStopSpeech = true

            if (!prefs.notificationReadAloudEnabled) {
                transientQueue.clear()
                shouldStopSpeech = true
                emptyList()
            } else {
                // Revalidate queued entries against current privacy settings, not only the
                // settings that were active when each notification arrived.
                val sizeBeforeAllowlist = transientQueue.size
                transientQueue.removeIf { it.packageName !in prefs.allowedNotificationPackages }
                if (transientQueue.size != sizeBeforeAllowlist) shouldStopSpeech = true

                if (prefs.notificationAnnounceSenderOnly) {
                    val containsPreview = transientQueue.any {
                        it.senderOrTitle.isNotBlank() || it.messageBody.isNotBlank()
                    }
                    if (containsPreview) shouldStopSpeech = true
                    val redactedItems = transientQueue.map {
                        it.copy(senderOrTitle = "", messageBody = "")
                    }
                    transientQueue.clear()
                    transientQueue.addAll(redactedItems)
                }
                transientQueue.toList()
            }
        }

        // Cancel already-enqueued previews whenever current privacy policy invalidates them.
        if (shouldStopSpeech) ttsController.stopSpeaking()

        if (snapshot.isEmpty()) {
            return ActionResult.Success(
                summaryEn = "No recent unread notifications in the active session queue.",
                summaryBn = "এই মুহূর্তে কোনো নতুন নোটিফিকেশন জমা নেই।",
                verifiedBySystem = true
            )
        }
        val latest = snapshot.last()
        val spokenEn = policy.formatSpokenAnnouncement(latest, prefs.notificationAnnounceSenderOnly, useBengali = false)
        val spokenBn = policy.formatSpokenAnnouncement(latest, prefs.notificationAnnounceSenderOnly, useBengali = true)
        return ActionResult.Success(
            summaryEn = "Latest of ${snapshot.size} notification(s): $spokenEn",
            summaryBn = "মোট ${snapshot.size}টি নোটিফিকেশনের সর্বশেষটি: $spokenBn",
            verifiedBySystem = true
        )
    }

    private fun pruneExpiredItems(nowMillis: Long) {
        val iterator = transientQueue.iterator()
        while (iterator.hasNext()) {
            val receivedAtMs = iterator.next().receivedAtMs
            if (!isRecent(receivedAtMs, nowMillis)) {
                iterator.remove()
            }
        }
    }

    private fun isRecent(receivedAtMs: Long, nowMillis: Long): Boolean =
        receivedAtMs <= nowMillis && nowMillis - receivedAtMs <= MAX_TRANSIENT_AGE_MS

    companion object {
        private const val MAX_TRANSIENT_ITEMS = 15
        const val MAX_TRANSIENT_AGE_MS = 15 * 60 * 1000L
    }
}
