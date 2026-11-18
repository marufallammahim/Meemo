package com.marufallammahim.meemo.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationProcessorTextTest {

    @Test
    fun `truncates a large notification extra before converting it to a string`() {
        val source = StringBuilder("x".repeat(100_000))

        val result = boundedNotificationText(source, maxLength = 240)

        assertEquals(240, result.length)
        assertTrue(result.all { it == 'x' })
    }

    @Test
    fun `trims bounded notification text and handles missing values`() {
        assertEquals("hello", boundedNotificationText("  hello  ", maxLength = 20))
        assertEquals("", boundedNotificationText(null, maxLength = 20))
        assertEquals("", boundedNotificationText("hello", maxLength = 0))
    }
    @Test
    fun `does not split supplementary Unicode characters at the length boundary`() {
        assertEquals("hello", boundedNotificationText("hello 😀world", maxLength = 7))
        assertEquals("A😀", boundedNotificationText("A😀B", maxLength = 3))
    }

}
