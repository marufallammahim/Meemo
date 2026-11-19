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
}
