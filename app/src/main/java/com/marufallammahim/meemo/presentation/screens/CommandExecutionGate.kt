package com.marufallammahim.meemo.presentation.screens

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/** Serializes assistant actions while retaining commands submitted during an active turn. */
internal class CommandExecutionGate {
    private val mutex = Mutex()
    private val pendingCommands = AtomicInteger(0)

    suspend fun runExclusive(
        onPendingChanged: (Boolean) -> Unit,
        block: suspend () -> Unit
    ) {
        onPendingChanged(pendingCommands.incrementAndGet() > 0)
        try {
            mutex.withLock { block() }
        } finally {
            onPendingChanged(pendingCommands.decrementAndGet() > 0)
        }
    }
}
