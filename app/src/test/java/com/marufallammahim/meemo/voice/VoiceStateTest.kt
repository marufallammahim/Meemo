package com.marufallammahim.meemo.voice

import com.marufallammahim.meemo.core.error.MeemoError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceStateTest {

    @Test
    fun `listening partial and stopping states retain an active session`() {
        assertTrue(VoiceRecognitionState.Listening("en-US").isSessionActive)
        assertTrue(VoiceRecognitionState.PartialTranscript("open settings").isSessionActive)
        assertTrue(VoiceRecognitionState.Stopping.isSessionActive)
    }

    @Test
    fun `idle final and error states do not expose an active session`() {
        assertFalse(VoiceRecognitionState.Idle.isSessionActive)
        assertFalse(VoiceRecognitionState.FinalTranscript("open settings").isSessionActive)
        assertFalse(
            VoiceRecognitionState.Error(MeemoError.SpeechUnavailable("network")).isSessionActive
        )
    }
}
