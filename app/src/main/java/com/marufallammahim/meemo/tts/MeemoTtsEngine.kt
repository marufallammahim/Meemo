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

@Singleton
class MeemoTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeLogger: SafeLogger
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val _state = MutableStateFlow(TtsEngineState.INITIALIZING)
    val state: StateFlow<TtsEngineState> = _state.asStateFlow()

    init {
        runCatching {
            tts = TextToSpeech(context, this)
        }.onFailure {
            _state.value = TtsEngineState.UNAVAILABLE
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _state.value = TtsEngineState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    _state.value = TtsEngineState.READY
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _state.value = TtsEngineState.READY
                }
            })
            _state.value = TtsEngineState.READY
        } else {
            _state.value = TtsEngineState.UNAVAILABLE
            safeLogger.warn("MeemoTtsEngine", "TTS initialization failed with status=$status")
        }
    }

    fun isLanguageSupported(languageTag: String): Boolean {
        val engine = tts ?: return false
        val locale = Locale.forLanguageTag(languageTag)
        val res = engine.isLanguageAvailable(locale)
        return res >= TextToSpeech.LANG_AVAILABLE
    }

    fun speak(
        text: String,
        languageTag: String = "en-US",
        speechRate: Float = 1.0f,
        flushQueue: Boolean = true
    ): Boolean {
        if (text.isBlank()) return false
        val engine = tts ?: return false
        if (_state.value == TtsEngineState.UNAVAILABLE) return false

        val locale = Locale.forLanguageTag(languageTag)
        val availability = engine.isLanguageAvailable(locale)
        if (availability < TextToSpeech.LANG_AVAILABLE) {
            safeLogger.warn("MeemoTtsEngine", "Locale $languageTag unavailable in installed TTS engine")
            return false
        }

        engine.language = locale
        engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        val queueMode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = UUID.randomUUID().toString()
        val result = engine.speak(text, queueMode, null, utteranceId)
        return result == TextToSpeech.SUCCESS
    }

    fun stop() {
        runCatching { tts?.stop() }
        if (_state.value == TtsEngineState.SPEAKING) {
            _state.value = TtsEngineState.READY
        }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        _state.value = TtsEngineState.UNAVAILABLE
    }
}
