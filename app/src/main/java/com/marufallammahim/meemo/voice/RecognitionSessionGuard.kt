package com.marufallammahim.meemo.voice

import java.util.concurrent.atomic.AtomicLong

/**
 * Identifies the currently active recognizer session so callbacks from cancelled,
 * replaced, or already completed recognizers cannot mutate the current UI state.
 */
internal class RecognitionSessionGuard {
    private val generation = AtomicLong(0L)

    fun beginSession(): Long = generation.incrementAndGet()

    fun isCurrentSession(sessionId: Long): Boolean = generation.get() == sessionId

    fun invalidateCurrentSession(): Long = generation.incrementAndGet()

    fun finishSession(sessionId: Long): Boolean =
        generation.compareAndSet(sessionId, sessionId + 1L)
}
