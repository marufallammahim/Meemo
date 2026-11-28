package com.marufallammahim.meemo.tts

import java.util.ArrayDeque

internal enum class TtsSpeechSource {
    ASSISTANT,
    NOTIFICATION
}

internal data class TtsSpeechRequest(
    val id: String,
    val text: String,
    val languageTag: String,
    val speechRate: Float,
    val source: TtsSpeechSource,
    val expiresAtMillis: Long? = null
)

internal data class TtsSpeechQueueAction(
    val stopActive: Boolean = false,
    val next: TtsSpeechRequest? = null
)

/**
 * Keeps Android's platform queue to one utterance at a time so notification
 * speech can be cancelled without interrupting an assistant response.
 */
internal class TtsSpeechQueue(
    private val maxNotificationRequests: Int = 15
) {
    private val pending = ArrayDeque<TtsSpeechRequest>()
    private var active: TtsSpeechRequest? = null

    @Synchronized
    fun enqueue(
        request: TtsSpeechRequest,
        replacePending: Boolean,
        nowMillis: Long
    ): TtsSpeechQueueAction {
        var stopActive = false
        if (replacePending) {
            pending.clear()
            stopActive = active != null
            active = null
        }

        if (isExpired(request, nowMillis)) {
            return TtsSpeechQueueAction(stopActive, activateNext(nowMillis))
        }

        if (request.source == TtsSpeechSource.NOTIFICATION) {
            val notificationCount = pending.count { it.source == TtsSpeechSource.NOTIFICATION } +
                if (active?.source == TtsSpeechSource.NOTIFICATION) 1 else 0
            if (notificationCount >= maxNotificationRequests) {
                if (active?.source == TtsSpeechSource.NOTIFICATION) {
                    active = null
                    stopActive = true
                } else {
                    removeOldestNotification()
                }
            }
        }

        pending.addLast(request)
        return TtsSpeechQueueAction(stopActive, activateNext(nowMillis))
    }

    @Synchronized
    fun complete(utteranceId: String?, nowMillis: Long): TtsSpeechQueueAction {
        if (utteranceId == null || active?.id != utteranceId) {
            return TtsSpeechQueueAction()
        }
        active = null
        return TtsSpeechQueueAction(next = activateNext(nowMillis))
    }

    @Synchronized
    fun stopNotifications(nowMillis: Long): TtsSpeechQueueAction {
        removePendingNotifications()
        val stopActive = active?.source == TtsSpeechSource.NOTIFICATION
        if (stopActive) active = null
        return TtsSpeechQueueAction(stopActive, activateNext(nowMillis))
    }

    @Synchronized
    fun stopAll(): TtsSpeechQueueAction {
        val stopActive = active != null
        pending.clear()
        active = null
        return TtsSpeechQueueAction(stopActive)
    }

    @Synchronized
    fun activeRequest(): TtsSpeechRequest? = active

    @Synchronized
    fun isActive(utteranceId: String?): Boolean =
        utteranceId != null && active?.id == utteranceId

    @Synchronized
    fun containsPending(utteranceId: String): Boolean =
        pending.any { it.id == utteranceId }

    @Synchronized
    fun pendingCount(): Int = pending.size

    private fun activateNext(nowMillis: Long): TtsSpeechRequest? {
        if (active != null) return null
        while (pending.isNotEmpty()) {
            val candidate = pending.removeFirst()
            if (!isExpired(candidate, nowMillis)) {
                active = candidate
                return candidate
            }
        }
        return null
    }

    private fun removeOldestNotification() {
        val iterator = pending.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().source == TtsSpeechSource.NOTIFICATION) {
                iterator.remove()
                return
            }
        }
    }

    private fun removePendingNotifications() {
        val iterator = pending.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().source == TtsSpeechSource.NOTIFICATION) {
                iterator.remove()
            }
        }
    }

    private fun isExpired(request: TtsSpeechRequest, nowMillis: Long): Boolean =
        request.expiresAtMillis?.let { it <= nowMillis } == true
}
