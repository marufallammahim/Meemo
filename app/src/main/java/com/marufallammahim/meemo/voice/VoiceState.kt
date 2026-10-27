package com.marufallammahim.meemo.voice

import com.marufallammahim.meemo.core.error.MeemoError

sealed interface VoiceRecognitionState {
    data object Idle : VoiceRecognitionState
    data class Listening(val localeTag: String) : VoiceRecognitionState
    data class PartialTranscript(val text: String) : VoiceRecognitionState
    data class FinalTranscript(val text: String) : VoiceRecognitionState
    data class Error(val error: MeemoError) : VoiceRecognitionState
}
