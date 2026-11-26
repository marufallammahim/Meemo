package com.marufallammahim.meemo.voice

import java.util.concurrent.atomic.AtomicLong

/**
 * Identifies the currently active recognizer session so callbacks from cancelled,
 * replaced, or already completed recognizers cannot mutate the current UI state.
 */
internal class RecognitionSessionGuard {
    private val generation = AtomicLong(0L)
    private val stopRequestedSession = AtomicLong(NO_SESSION)

    fun beginSession(): Long {
        val sessionId = generation.incrementAndGet()
        stopRequestedSession.set(NO_SESSION)
        return sessionId
    }

    fun isCurrentSession(sessionId: Long): Boolean = generation.get() == sessionId

    fun requestStopForCurrentSession(): Boolean {
        val sessionId = generation.get()
        if (sessionId == NO_SESSION) return false

        stopRequestedSession.set(sessionId)
        if (generation.get() == sessionId) return true

        stopRequestedSession.compareAndSet(sessionId, NO_SESSION)
        return false
    }

    fun consumeStopRequest(sessionId: Long): Boolean =
        stopRequestedSession.compareAndSet(sessionId, NO_SESSION)

    fun invalidateCurrentSession(): Long {
        stopRequestedSession.set(NO_SESSION)
        return generation.incrementAndGet()
    }

    fun finishSession(sessionId: Long): Boolean =
        generation.compareAndSet(sessionId, sessionId + 1L)

    private companion object {
        const val NO_SESSION = -1L
    }
}
