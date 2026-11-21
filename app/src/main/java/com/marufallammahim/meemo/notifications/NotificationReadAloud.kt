package com.marufallammahim.meemo.notifications

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.tts.TtsController
import kotlinx.coroutines.flow.first
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a bounded transient in-memory queue (max 15 items) for notification read-aloud.
 * Never persists notification text to Room or disk.
 */
@Singleton
class NotificationReadAloud @Inject constructor(
    private val policy: NotificationPolicy,
    private val ttsController: TtsController,
    private val repository: MeemoRepository
) {
    private val transientQueue = ArrayDeque<TransientNotificationItem>()

    @Synchronized
    fun enqueueTransient(item: TransientNotificationItem) {
        while (transientQueue.size >= MAX_TRANSIENT_ITEMS) {
            transientQueue.pollFirst()
        }
        transientQueue.addLast(item)
    }

    @Synchronized
    fun clearTransientQueue() {
        transientQueue.clear()
    }

    suspend fun handleIncomingNotification(item: TransientNotificationItem) {
        val prefs = repository.userPreferencesFlow.first()
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
        val snapshot: List<TransientNotificationItem>
        synchronized(this) {
            snapshot = transientQueue.toList()
        }
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

    companion object {
        private const val MAX_TRANSIENT_ITEMS = 15
    }
}
