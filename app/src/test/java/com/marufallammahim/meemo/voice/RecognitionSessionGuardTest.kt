package com.marufallammahim.meemo.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionSessionGuardTest {

    @Test
    fun `invalidated session cannot publish callbacks after cancellation`() {
        val guard = RecognitionSessionGuard()
        val cancelledSession = guard.beginSession()

        guard.invalidateCurrentSession()

        assertFalse(guard.isCurrentSession(cancelledSession))
        val nextSession = guard.beginSession()
        assertTrue(guard.isCurrentSession(nextSession))
    }

    @Test
    fun `finishing a session rejects duplicate and stale terminal callbacks`() {
        val guard = RecognitionSessionGuard()
        val firstSession = guard.beginSession()

        assertTrue(guard.finishSession(firstSession))
        assertFalse(guard.finishSession(firstSession))

        val secondSession = guard.beginSession()
        assertTrue(guard.isCurrentSession(secondSession))
        assertFalse(guard.finishSession(firstSession))
        assertTrue(guard.isCurrentSession(secondSession))
    }

    @Test
    fun `does not request a stop when no recognizer session is active`() {
        val guard = RecognitionSessionGuard()

        assertFalse(guard.requestStopForCurrentSession())
    }

    @Test
    fun `records an explicit stop only for the current session`() {
        val guard = RecognitionSessionGuard()
        val sessionId = guard.beginSession()

        assertTrue(guard.requestStopForCurrentSession())
        assertTrue(guard.isCurrentSession(sessionId))
        assertTrue(guard.isStopRequested(sessionId))
        assertTrue(guard.consumeStopRequest(sessionId))
        assertFalse(guard.isStopRequested(sessionId))
        assertFalse(guard.consumeStopRequest(sessionId))
    }

    @Test
    fun `new session does not inherit a previous stop request`() {
        val guard = RecognitionSessionGuard()
        val firstSession = guard.beginSession()

        assertTrue(guard.requestStopForCurrentSession())
        assertTrue(guard.finishSession(firstSession))

        val nextSession = guard.beginSession()
        assertTrue(guard.isCurrentSession(nextSession))
        assertFalse(guard.consumeStopRequest(nextSession))
    }
}
