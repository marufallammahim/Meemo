package com.marufallammahim.meemo.presentation.screens

import java.util.concurrent.atomic.AtomicBoolean

/** Prevents two assistant actions from running concurrently in the same screen. */
internal class CommandExecutionGate {
    private val isActive = AtomicBoolean(false)

    fun tryEnter(): Boolean = isActive.compareAndSet(false, true)

    fun leave() {
        isActive.set(false)
    }
}
