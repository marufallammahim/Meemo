package com.marufallammahim.meemo.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.marufallammahim.meemo.core.logging.SafeLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class TtsEngineState {
    INITIALIZING,
    READY,
    SPEAKING,
    UNAVAILABLE
}

internal fun TtsEngineState.acceptsSpeech(): Boolean =
    this == TtsEngineState.READY || this == TtsEngineState.SPEAKING

internal class PendingUtteranceQueue {
    private val pendingIds = mutableSetOf<String>()

    @Synchronized
    fun enqueue(utteranceId: String, flushQueue: Boolean) {
        if (flushQueue) pendingIds.clear()
        pendingIds.add(utteranceId)
    }

    @Synchronized
    fun contains(utteranceId: String?): Boolean =
        utteranceId != null && pendingIds.contains(utteranceId)

    @Synchronized
    fun complete(utteranceId: String?): Boolean {
        if (utteranceId != null) pendingIds.remove(utteranceId)
        return pendingIds.isEmpty()
    }

    @Synchronized
    fun clear() {
        pendingIds.clear()
    }

    @Synchronized
    fun isEmpty(): Boolean = pendingIds.isEmpty()
}

@Singleton
class MeemoTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeLogger: SafeLogger
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val pendingUtterances = PendingUtteranceQueue()
    private val _state = MutableStateFlow(TtsEngineState.INITIALIZING)
    val state: StateFlow<TtsEngineState> = _state.asStateFlow()

    init {
        runCatching {
            tts = TextToSpeech(context, this)
        }.onFailure {
            _state.value = TtsEngineState.UNAVAILABLE
        }
    }

    @Synchronized
    override fun onInit(status: Int) {
        // TextToSpeech initialization is asynchronous. A callback can arrive after
        // shutdown(), so never let a stale callback resurrect a released engine.
        val engine = tts ?: return
        if (status == TextToSpeech.SUCCESS) {
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (pendingUtterances.contains(utteranceId)) {
                        _state.value = TtsEngineState.SPEAKING
                    }
                }

                override fun onDone(utteranceId: String?) {
                    updateStateAfterCompletion(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    updateStateAfterCompletion(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    safeLogger.warn("MeemoTtsEngine", "TTS utterance failed with error=$errorCode")
                    updateStateAfterCompletion(utteranceId)
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    updateStateAfterCompletion(utteranceId)
                }
            })
            if (pendingUtterances.isEmpty()) {
                _state.value = TtsEngineState.READY
            }
        } else {
            _state.value = TtsEngineState.UNAVAILABLE
            safeLogger.warn("MeemoTtsEngine", "TTS initialization failed with status=$status")
        }
    }

    @Synchronized
    fun isLanguageSupported(languageTag: String): Boolean {
        if (!_state.value.acceptsSpeech()) return false
        val engine = tts ?: return false
        val locale = Locale.forLanguageTag(languageTag)
        val res = engine.isLanguageAvailable(locale)
        return res >= TextToSpeech.LANG_AVAILABLE
    }

    @Synchronized
    fun speak(
        text: String,
        languageTag: String = "en-US",
        speechRate: Float = 1.0f,
        flushQueue: Boolean = true
    ): Boolean {
        if (text.isBlank() || !_state.value.acceptsSpeech()) return false
        val engine = tts ?: return false

        val locale = Locale.forLanguageTag(languageTag)
        val availability = engine.isLanguageAvailable(locale)
        if (availability < TextToSpeech.LANG_AVAILABLE) {
            safeLogger.warn("MeemoTtsEngine", "Locale $languageTag unavailable in installed TTS engine")
            return false
        }

        engine.language = locale
        engine.setSpeechRate(normalizeSpeechRate(speechRate))
        val queueMode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = UUID.randomUUID().toString()
        pendingUtterances.enqueue(utteranceId, flushQueue)
        val result = engine.speak(text, queueMode, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            updateStateAfterCompletion(utteranceId)
        }
        return result == TextToSpeech.SUCCESS
    }

    @Synchronized
    fun stop() {
        pendingUtterances.clear()
        runCatching { tts?.stop() }
        if (_state.value == TtsEngineState.SPEAKING) {
            _state.value = TtsEngineState.READY
        }
    }

    @Synchronized
    fun shutdown() {
        pendingUtterances.clear()
        val engine = tts
        tts = null
        runCatching {
            engine?.stop()
            engine?.shutdown()
        }
        _state.value = TtsEngineState.UNAVAILABLE
    }

    private fun updateStateAfterCompletion(utteranceId: String?) {
        if (pendingUtterances.complete(utteranceId) && _state.value == TtsEngineState.SPEAKING) {
            _state.value = TtsEngineState.READY
        }
    }

    companion object {
        internal fun normalizeSpeechRate(value: Float): Float =
            if (value.isFinite()) value.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE) else DEFAULT_SPEECH_RATE

        private const val MIN_SPEECH_RATE = 0.5f
        private const val MAX_SPEECH_RATE = 2.0f
        private const val DEFAULT_SPEECH_RATE = 1.0f
    }
}
