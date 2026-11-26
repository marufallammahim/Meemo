package com.marufallammahim.meemo.presentation.screens

import com.marufallammahim.meemo.assistant.engine.AssistantExecutionState
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceRecognitionStateReducerTest {

    @Test
    fun `returns to idle after listening or transcription ends without a result`() {
        assertEquals(
            AssistantExecutionState.IDLE,
            executionStateAfterVoiceIdle(AssistantExecutionState.LISTENING)
        )
        assertEquals(
            AssistantExecutionState.IDLE,
            executionStateAfterVoiceIdle(AssistantExecutionState.TRANSCRIBING)
        )
    }

    @Test
    fun `preserves command execution states when recognition becomes idle`() {
        assertEquals(
            AssistantExecutionState.EXECUTING,
            executionStateAfterVoiceIdle(AssistantExecutionState.EXECUTING)
        )
        assertEquals(
            AssistantExecutionState.SUCCESS,
            executionStateAfterVoiceIdle(AssistantExecutionState.SUCCESS)
        )
    }
}
