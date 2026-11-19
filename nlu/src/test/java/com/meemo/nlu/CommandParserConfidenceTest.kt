package com.meemo.nlu

import com.meemo.core.model.MeemoIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserConfidenceTest {
    @Test
    fun fuzzyBatteryTypoGetsConfirmationOrExecutionConfidence() {
        val command = CommandParser().parse("battrey")
        assertEquals(MeemoIntent.GetBattery, command.intent)
        assertTrue(command.confidence in 0.6f..0.9f)
    }

    @Test
    fun fuzzyBrowserTypoGetsBrowserIntent() {
        val command = CommandParser().parse("open browsr")
        assertEquals(MeemoIntent.OpenBrowser, command.intent)
        assertTrue(command.confidence in 0.6f..0.9f)
    }
}
