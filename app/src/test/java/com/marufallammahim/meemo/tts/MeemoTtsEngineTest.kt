package com.marufallammahim.meemo.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeemoTtsEngineTest {

    @Test
    fun `clamps finite speech rates to supported range`() {
        assertEquals(0.5f, MeemoTtsEngine.normalizeSpeechRate(0.1f), 0.0f)
        assertEquals(1.5f, MeemoTtsEngine.normalizeSpeechRate(1.5f), 0.0f)
        assertEquals(2.0f, MeemoTtsEngine.normalizeSpeechRate(3.0f), 0.0f)
    }

    @Test
    fun `falls back to normal rate for non finite speech rates`() {
        assertEquals(1.0f, MeemoTtsEngine.normalizeSpeechRate(Float.NaN), 0.0f)
        assertEquals(1.0f, MeemoTtsEngine.normalizeSpeechRate(Float.POSITIVE_INFINITY), 0.0f)
        assertEquals(1.0f, MeemoTtsEngine.normalizeSpeechRate(Float.NEGATIVE_INFINITY), 0.0f)
    }

    @Test
    fun `only ready or speaking engines accept speech requests`() {
        assertFalse(TtsEngineState.INITIALIZING.acceptsSpeech())
        assertTrue(TtsEngineState.READY.acceptsSpeech())
        assertTrue(TtsEngineState.SPEAKING.acceptsSpeech())
        assertFalse(TtsEngineState.UNAVAILABLE.acceptsSpeech())
    }

    @Test
    fun `keeps speaking state pending until every queued utterance completes`() {
        val queue = PendingUtteranceQueue()
        queue.enqueue("first", flushQueue = true)
        queue.enqueue("second", flushQueue = false)

        assertFalse(queue.complete("first"))
        assertTrue(queue.contains("second"))
        assertTrue(queue.complete("second"))
    }

    @Test
    fun `queue flush replaces prior utterance tracking and ignores stale callbacks`() {
        val queue = PendingUtteranceQueue()
        queue.enqueue("old", flushQueue = true)
        queue.enqueue("replacement", flushQueue = true)

        assertFalse(queue.contains("old"))
        assertFalse(queue.complete("old"))
        assertTrue(queue.complete("replacement"))
    }

    @Test
    fun `stop clears all pending utterances`() {
        val queue = PendingUtteranceQueue()
        queue.enqueue("first", flushQueue = true)
        queue.enqueue("second", flushQueue = false)

        queue.clear()

        assertTrue(queue.isEmpty())
        assertTrue(queue.complete("first"))
    }
}
