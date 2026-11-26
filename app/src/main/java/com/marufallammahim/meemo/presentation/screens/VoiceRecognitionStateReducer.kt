package com.marufallammahim.meemo.presentation.screens

import com.marufallammahim.meemo.assistant.engine.AssistantExecutionState

internal fun executionStateAfterVoiceIdle(
    current: AssistantExecutionState
): AssistantExecutionState = when (current) {
    AssistantExecutionState.LISTENING,
    AssistantExecutionState.TRANSCRIBING -> AssistantExecutionState.IDLE
    else -> current
}
