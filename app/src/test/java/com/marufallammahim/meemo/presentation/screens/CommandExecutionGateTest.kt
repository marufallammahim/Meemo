package com.marufallammahim.meemo.presentation.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandExecutionGateTest {

    @Test
    fun `rejects a second command while one is active`() {
        val gate = CommandExecutionGate()

        assertTrue(gate.tryEnter())
        assertFalse(gate.tryEnter())
    }

    @Test
    fun `allows the next command after the active command leaves`() {
        val gate = CommandExecutionGate()

        assertTrue(gate.tryEnter())
        gate.leave()

        assertTrue(gate.tryEnter())
        assertFalse(gate.tryEnter())
    }
}
