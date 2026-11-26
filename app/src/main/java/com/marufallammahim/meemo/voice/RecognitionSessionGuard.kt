package com.marufallammahim.meemo.voice

import java.util.concurrent.atomic.AtomicLong

/**
 * Identifies the currently active recognizer session so callbacks from cancelled,
 * replaced, or already completed recognizers cannot mutate the current UI state.
 */
internal class RecognitionSessionGuard {
    private val generation = AtomicLong(0L)
    private val activeSession = AtomicLong(NO_SESSION)
    private val stopRequestedSession = AtomicLong(NO_SESSION)

    fun beginSession(): Long {
        val sessionId = generation.incrementAndGet()
        stopRequestedSession.set(NO_SESSION)
        activeSession.set(sessionId)
        return sessionId
    }

    fun isCurrentSession(sessionId: Long): Boolean = activeSession.get() == sessionId

    fun requestStopForCurrentSession(): Boolean {
        val sessionId = activeSession.get()
        if (sessionId == NO_SESSION) return false

        stopRequestedSession.set(sessionId)
        if (activeSession.get() == sessionId) return true

        stopRequestedSession.compareAndSet(sessionId, NO_SESSION)
        return false
    }

    fun consumeStopRequest(sessionId: Long): Boolean =
        stopRequestedSession.compareAndSet(sessionId, NO_SESSION)

    fun invalidateCurrentSession(): Long {
        activeSession.set(NO_SESSION)
        stopRequestedSession.set(NO_SESSION)
        return generation.incrementAndGet()
    }

    fun finishSession(sessionId: Long): Boolean =
        activeSession.compareAndSet(sessionId, NO_SESSION)

    private companion object {
        const val NO_SESSION = -1L
    }
}
