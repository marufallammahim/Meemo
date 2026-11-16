package com.marufallammahim.meemo.assistant.context

import com.marufallammahim.meemo.core.utilities.TimeProvider
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationContextManagerTest {

    private class FakeTimeProvider(var now: Long = 10_000L) : TimeProvider {
        override fun nowMillis(): Long = now
        override fun currentHourOfDay(): Int = 12
    }

    @Test
    fun `suppresses same normalized command inside debounce window`() {
        val time = FakeTimeProvider()
        val manager = ConversationContextManager(time)

        assertFalse(manager.isDuplicateSubmission("  Open Chrome  "))
        time.now += 500L
        assertTrue(manager.isDuplicateSubmission("open chrome"))
    }

    @Test
    fun `allows same command after debounce window expires`() {
        val time = FakeTimeProvider()
        val manager = ConversationContextManager(time)

        assertFalse(manager.isDuplicateSubmission("open Chrome"))
        time.now += ConversationContextManager.DUPLICATE_DEBOUNCE_WINDOW_MS + 1
        assertFalse(manager.isDuplicateSubmission("open Chrome"))
    }

    @Test
    fun `new session clears duplicate history and changes session id`() {
        val time = FakeTimeProvider()
        val manager = ConversationContextManager(time)
        val previousSessionId = manager.currentSessionId()

        manager.isDuplicateSubmission("open Chrome")
        val nextSessionId = manager.startNewSession()

        assertNotEquals(previousSessionId, nextSessionId)
        assertFalse(manager.isDuplicateSubmission("open Chrome"))
    }

    @Test
    fun `does not treat blank commands as duplicate submissions`() {
        val manager = ConversationContextManager(FakeTimeProvider())

        assertFalse(manager.isDuplicateSubmission(" "))
        assertFalse(manager.isDuplicateSubmission(""))
    }
}
