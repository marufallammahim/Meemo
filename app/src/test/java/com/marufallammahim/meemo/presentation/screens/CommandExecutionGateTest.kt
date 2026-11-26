package com.marufallammahim.meemo.presentation.screens

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandExecutionGateTest {

    @Test
    fun `serializes commands submitted while another command is active`() = runTest {
        val gate = CommandExecutionGate()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val pendingStates = mutableListOf<Boolean>()

        val first = launch {
            gate.runExclusive(
                onPendingChanged = pendingStates::add
            ) {
                events += "first-start"
                firstStarted.complete(Unit)
                releaseFirst.await()
                events += "first-end"
            }
        }
        firstStarted.await()

        val second = launch {
            gate.runExclusive(
                onPendingChanged = pendingStates::add
            ) {
                events += "second"
            }
        }
        yield()

        assertEquals(listOf("first-start"), events)
        assertTrue(pendingStates.all { it })

        releaseFirst.complete(Unit)
        joinAll(first, second)

        assertEquals(listOf("first-start", "first-end", "second"), events)
        assertEquals(listOf(true, true, true, false), pendingStates)
    }

    @Test
    fun `releases the gate after a command throws`() = runTest {
        val gate = CommandExecutionGate()
        val failed = async {
            runCatching {
                gate.runExclusive(onPendingChanged = {}) {
                    error("expected failure")
                }
            }
        }

        assertTrue(failed.await().isFailure)

        var nextCommandRan = false
        gate.runExclusive(onPendingChanged = {}) {
            nextCommandRan = true
        }
        assertTrue(nextCommandRan)
    }
}
