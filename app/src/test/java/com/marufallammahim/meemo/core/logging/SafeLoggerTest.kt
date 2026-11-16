package com.marufallammahim.meemo.core.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeLoggerTest {

    private val logger = SafeLogger()

    @Test
    fun `redacts Bangladesh and international phone numbers`() {
        val result = SafeLogger.redactSensitiveContent(
            "Call 01712345678 or +14155552671 for details"
        )

        assertFalse(result.contains("01712345678"))
        assertFalse(result.contains("+14155552671"))
        assertEquals(2, Regex("\\\\[REDACTED_PHONE]").findAll(result).count())
    }

    @Test
    fun `bounds stored message and tag lengths`() {
        logger.info("T".repeat(100), "M".repeat(3_000))

        val entry = logger.getRecentLogs().single()
        assertEquals(64, entry.tag.length)
        assertEquals(2_000, entry.redactedMessage.length)
    }

    @Test
    fun `uses fallback tag when supplied tag is blank`() {
        logger.warn("   ", "safe message")

        assertEquals("Meemo", logger.getRecentLogs().single().tag)
    }
}
