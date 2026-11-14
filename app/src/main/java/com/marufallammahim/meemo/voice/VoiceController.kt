package com.marufallammahim.meemo.voice

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceController @Inject constructor(
    private val speechRecognizerController: SpeechRecognizerController
) {
    val recognitionState: StateFlow<VoiceRecognitionState> = speechRecognizerController.state

    fun startVoiceCapture(localeTag: String) = speechRecognizerController.startListening(localeTag)
    fun stopVoiceCapture() = speechRecognizerController.stopListening()
    fun cancelVoiceCapture() = speechRecognizerController.cancelAndRelease()
}
