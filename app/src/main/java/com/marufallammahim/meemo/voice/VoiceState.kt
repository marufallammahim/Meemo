package com.marufallammahim.meemo.voice

import com.marufallammahim.meemo.core.error.MeemoError

sealed interface VoiceRecognitionState {
    data object Idle : VoiceRecognitionState
    data class Listening(val localeTag: String) : VoiceRecognitionState
    data object Stopping : VoiceRecognitionState
    data class PartialTranscript(val text: String) : VoiceRecognitionState
    data class FinalTranscript(val text: String) : VoiceRecognitionState
    data class Error(val error: MeemoError) : VoiceRecognitionState
}

/**
 * Whether this state still owns an active platform recognition session.
 *
 * A partial transcript and a pending stop both belong to the current session:
 * the UI must keep exposing Stop instead of requesting microphone permission again.
 */
val VoiceRecognitionState.isSessionActive: Boolean
    get() = this is VoiceRecognitionState.Listening ||
        this is VoiceRecognitionState.PartialTranscript ||
        this is VoiceRecognitionState.Stopping
