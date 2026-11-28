package com.marufallammahim.meemo.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `assistant flush replaces the active utterance and queued requests`() {
        val queue = TtsSpeechQueue()
        val first = queue.enqueue(request("first", TtsSpeechSource.ASSISTANT), false, 0L)
        queue.enqueue(request("notification", TtsSpeechSource.NOTIFICATION), false, 0L)

        val replacement = queue.enqueue(
            request("replacement", TtsSpeechSource.ASSISTANT),
            replacePending = true,
            nowMillis = 0L
        )

        assertTrue(replacement.stopActive)
        assertEquals("replacement", replacement.next?.id)
        assertFalse(queue.isActive("first"))
        assertFalse(queue.containsPending("notification"))
        assertEquals("replacement", queue.activeRequest()?.id)
        assertNull(queue.complete("first", 0L).next)
        assertEquals("replacement", queue.activeRequest()?.id)
        assertEquals("first", first.next?.id)
    }

    @Test
    fun `notification privacy cancellation leaves an assistant response untouched`() {
        val queue = TtsSpeechQueue()
        queue.enqueue(request("assistant", TtsSpeechSource.ASSISTANT), false, 0L)
        queue.enqueue(request("notification-one", TtsSpeechSource.NOTIFICATION), false, 0L)
        queue.enqueue(request("notification-two", TtsSpeechSource.NOTIFICATION), false, 0L)

        val cancellation = queue.stopNotifications(0L)

        assertFalse(cancellation.stopActive)
        assertNull(cancellation.next)
        assertEquals("assistant", queue.activeRequest()?.id)
        assertEquals(0, queue.pendingCount())
    }

    @Test
    fun `cancelling an active notification advances to the next retained request`() {
        val queue = TtsSpeechQueue()
        queue.enqueue(request("notification", TtsSpeechSource.NOTIFICATION), false, 0L)
        queue.enqueue(request("assistant", TtsSpeechSource.ASSISTANT), false, 0L)

        val cancellation = queue.stopNotifications(0L)

        assertTrue(cancellation.stopActive)
        assertEquals("assistant", cancellation.next?.id)
        assertEquals("assistant", queue.activeRequest()?.id)
    }

    @Test
    fun `notification capacity evicts the oldest preview without stopping assistant speech`() {
        val queue = TtsSpeechQueue(maxNotificationRequests = 2)
        queue.enqueue(request("assistant", TtsSpeechSource.ASSISTANT), false, 0L)
        queue.enqueue(request("oldest", TtsSpeechSource.NOTIFICATION), false, 0L)
        queue.enqueue(request("middle", TtsSpeechSource.NOTIFICATION), false, 0L)

        val newest = queue.enqueue(request("newest", TtsSpeechSource.NOTIFICATION), false, 0L)

        assertFalse(newest.stopActive)
        assertNull(newest.next)
        assertEquals("assistant", queue.activeRequest()?.id)
        assertFalse(queue.containsPending("oldest"))
        assertTrue(queue.containsPending("middle"))
        assertTrue(queue.containsPending("newest"))
        assertEquals(2, queue.pendingCount())
    }

    @Test
    fun `expired notification requests are skipped when the active utterance completes`() {
        val queue = TtsSpeechQueue()
        queue.enqueue(request("assistant", TtsSpeechSource.ASSISTANT), false, 0L)
        queue.enqueue(
            request("expired", TtsSpeechSource.NOTIFICATION, expiresAtMillis = 10L),
            false,
            0L
        )
        queue.enqueue(
            request("fresh", TtsSpeechSource.NOTIFICATION, expiresAtMillis = 100L),
            false,
            0L
        )

        val next = queue.complete("assistant", 10L)

        assertEquals("fresh", next.next?.id)
        assertEquals("fresh", queue.activeRequest()?.id)
        assertFalse(queue.containsPending("expired"))
    }

    @Test
    fun `stale completion callbacks cannot advance a replacement utterance`() {
        val queue = TtsSpeechQueue()
        queue.enqueue(request("old", TtsSpeechSource.NOTIFICATION), false, 0L)
        queue.enqueue(request("new", TtsSpeechSource.ASSISTANT), true, 0L)

        val staleCompletion = queue.complete("old", 0L)

        assertNull(staleCompletion.next)
        assertEquals("new", queue.activeRequest()?.id)
    }

    private fun request(
        id: String,
        source: TtsSpeechSource,
        expiresAtMillis: Long? = null
    ) = TtsSpeechRequest(
        id = id,
        text = "speech",
        languageTag = "en-US",
        speechRate = 1.0f,
        source = source,
        expiresAtMillis = expiresAtMillis
    )
}
