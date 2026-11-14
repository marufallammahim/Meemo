package com.marufallammahim.meemo.tts

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsController @Inject constructor(
    private val ttsEngine: MeemoTtsEngine
) {
    val ttsState: StateFlow<TtsEngineState> = ttsEngine.state

    fun speakResponse(
        text: String,
        languageTag: String,
        speechRate: Float,
        ttsEnabled: Boolean
    ): Boolean {
        if (!ttsEnabled || text.isBlank()) return false
        return ttsEngine.speak(
            text = text,
            languageTag = languageTag,
            speechRate = speechRate,
            flushQueue = true
        )
    }

    fun enqueueNotificationSpeech(
        text: String,
        languageTag: String,
        speechRate: Float
    ): Boolean {
        return ttsEngine.speak(
            text = text,
            languageTag = languageTag,
            speechRate = speechRate,
            flushQueue = false
        )
    }

    fun stopSpeaking() {
        ttsEngine.stop()
    }
}
