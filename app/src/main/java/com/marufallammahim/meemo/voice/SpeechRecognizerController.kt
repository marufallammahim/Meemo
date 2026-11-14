package com.marufallammahim.meemo.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.marufallammahim.meemo.core.error.MeemoError
import com.marufallammahim.meemo.core.logging.SafeLogger
import com.marufallammahim.meemo.core.permissions.PermissionChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lifecycle-aware SpeechRecognizer controller supporting English (en-US) and Bengali (bn-BD).
 * Prevents overlapping recognition sessions and maps recognizer error codes cleanly.
 */
@Singleton
class SpeechRecognizerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionChecker: PermissionChecker,
    private val safeLogger: SafeLogger
) {
    private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
    val state: StateFlow<VoiceRecognitionState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val isSessionActive = AtomicBoolean(false)

    fun isRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(localeTag: String = "en-US") {
        if (!permissionChecker.hasRecordAudioPermission()) {
            _state.value = VoiceRecognitionState.Error(
                MeemoError.PermissionDenied(
                    permissionName = android.Manifest.permission.RECORD_AUDIO,
                    rationaleEn = "Microphone permission is required to listen for voice commands.",
                    rationaleBn = "ভয়েস কমান্ড শোনার জন্য মাইক্রোফোন অনুমতি প্রয়োজন।"
                )
            )
            return
        }
        if (!isRecognitionAvailable()) {
            _state.value = VoiceRecognitionState.Error(
                MeemoError.SpeechUnavailable("No speech recognition service installed on device")
            )
            return
        }
        if (!isSessionActive.compareAndSet(false, true)) {
            safeLogger.info("SpeechController", "Ignored overlapping startListening request")
            return
        }

        try {
            releaseRecognizer()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(createListener())

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            _state.value = VoiceRecognitionState.Listening(localeTag)
            recognizer.startListening(intent)
        } catch (e: Exception) {
            isSessionActive.set(false)
            safeLogger.error("SpeechController", "Failed to start SpeechRecognizer", e)
            _state.value = VoiceRecognitionState.Error(
                MeemoError.SpeechUnavailable(e.javaClass.simpleName)
            )
        }
    }

    fun stopListening() {
        isSessionActive.set(false)
        runCatching { speechRecognizer?.stopListening() }
        _state.value = VoiceRecognitionState.Idle
    }

    fun cancelAndRelease() {
        isSessionActive.set(false)
        releaseRecognizer()
        _state.value = VoiceRecognitionState.Idle
    }

    private fun releaseRecognizer() {
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isSessionActive.set(false)
        }

        override fun onError(error: Int) {
            isSessionActive.set(false)
            val reason = mapRecognizerErrorCode(error)
            _state.value = VoiceRecognitionState.Error(MeemoError.SpeechUnavailable(reason))
        }

        override fun onResults(results: Bundle?) {
            isSessionActive.set(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = matches?.firstOrNull()?.trim().orEmpty()
            if (best.isBlank()) {
                _state.value = VoiceRecognitionState.Error(MeemoError.EmptyCommand)
            } else {
                _state.value = VoiceRecognitionState.FinalTranscript(best)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (partial.isNotBlank()) {
                _state.value = VoiceRecognitionState.PartialTranscript(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object {
        fun mapRecognizerErrorCode(errorCode: Int): String = when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side speech error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission missing"
            SpeechRecognizer.ERROR_NETWORK -> "Network error during recognition"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout during recognition"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            else -> "Speech error ($errorCode)"
        }
    }
}
