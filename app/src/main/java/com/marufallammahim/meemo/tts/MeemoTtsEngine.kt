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

@Singleton
class MeemoTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeLogger: SafeLogger
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val speechQueue = TtsSpeechQueue()
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
        // A callback can arrive after shutdown(), so never resurrect a released engine.
        val engine = tts ?: return
        if (status == TextToSpeech.SUCCESS) {
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    synchronized(this@MeemoTtsEngine) {
                        if (speechQueue.isActive(utteranceId)) {
                            _state.value = TtsEngineState.SPEAKING
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    completeUtterance(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    completeUtterance(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    safeLogger.warn("MeemoTtsEngine", "TTS utterance failed with error=$errorCode")
                    completeUtterance(utteranceId)
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    completeUtterance(utteranceId)
                }
            })
            _state.value = TtsEngineState.READY
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
        return engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
    }

    @Synchronized
    fun speak(
        text: String,
        languageTag: String = "en-US",
        speechRate: Float = 1.0f,
        flushQueue: Boolean = true
    ): Boolean = enqueueRequest(
        text = text,
        languageTag = languageTag,
        speechRate = speechRate,
        source = TtsSpeechSource.ASSISTANT,
        expiresAtMillis = null,
        replacePending = flushQueue
    )

    @Synchronized
    fun enqueueNotificationSpeech(
        text: String,
        languageTag: String,
        speechRate: Float,
        expiresAtMillis: Long
    ): Boolean = enqueueRequest(
        text = text,
        languageTag = languageTag,
        speechRate = speechRate,
        source = TtsSpeechSource.NOTIFICATION,
        expiresAtMillis = expiresAtMillis,
        replacePending = false
    )

    @Synchronized
    fun stopNotificationSpeech() {
        val engine = tts ?: return
        val action = speechQueue.stopNotifications(System.currentTimeMillis())
        executeActionLocked(engine, action)
        if (speechQueue.activeRequest() == null && _state.value == TtsEngineState.SPEAKING) {
            _state.value = TtsEngineState.READY
        }
    }

    @Synchronized
    fun stop() {
        speechQueue.stopAll()
        runCatching { tts?.stop() }
        if (tts != null && _state.value != TtsEngineState.UNAVAILABLE) {
            _state.value = TtsEngineState.READY
        }
    }

    @Synchronized
    fun shutdown() {
        speechQueue.stopAll()
        val engine = tts
        tts = null
        runCatching {
            engine?.stop()
            engine?.shutdown()
        }
        _state.value = TtsEngineState.UNAVAILABLE
    }

    private fun enqueueRequest(
        text: String,
        languageTag: String,
        speechRate: Float,
        source: TtsSpeechSource,
        expiresAtMillis: Long?,
        replacePending: Boolean
    ): Boolean {
        if (text.isBlank() || !_state.value.acceptsSpeech()) return false
        val engine = tts ?: return false
        val request = TtsSpeechRequest(
            id = UUID.randomUUID().toString(),
            text = text,
            languageTag = languageTag,
            speechRate = speechRate,
            source = source,
            expiresAtMillis = expiresAtMillis
        )
        val action = speechQueue.enqueue(request, replacePending, System.currentTimeMillis())
        executeActionLocked(engine, action)
        return speechQueue.isActive(request.id) || speechQueue.containsPending(request.id)
    }

    private fun executeActionLocked(engine: TextToSpeech, action: TtsSpeechQueueAction) {
        if (action.stopActive) {
            runCatching { engine.stop() }
            if (_state.value == TtsEngineState.SPEAKING) {
                _state.value = TtsEngineState.READY
            }
        }
        action.next?.let { startRequestLocked(engine, it) }
    }

    private fun startRequestLocked(engine: TextToSpeech, request: TtsSpeechRequest) {
        if (!speechQueue.isActive(request.id)) return
        val nowMillis = System.currentTimeMillis()
        if (request.expiresAtMillis?.let { it <= nowMillis } == true) {
            executeActionLocked(engine, speechQueue.complete(request.id, nowMillis))
            return
        }

        val locale = Locale.forLanguageTag(request.languageTag)
        if (engine.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) {
            safeLogger.warn(
                "MeemoTtsEngine",
                "Locale ${request.languageTag} unavailable in installed TTS engine"
            )
            executeActionLocked(engine, speechQueue.complete(request.id, nowMillis))
            return
        }

        engine.language = locale
        engine.setSpeechRate(normalizeSpeechRate(request.speechRate))
        // Only one utterance is submitted to Android at a time; the app queue owns ordering.
        val result = engine.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, request.id)
        if (result != TextToSpeech.SUCCESS) {
            safeLogger.warn("MeemoTtsEngine", "TTS engine rejected an utterance")
            executeActionLocked(engine, speechQueue.complete(request.id, System.currentTimeMillis()))
        }
    }

    private fun completeUtterance(utteranceId: String?) {
        synchronized(this) {
            val engine = tts ?: return
            val action = speechQueue.complete(utteranceId, System.currentTimeMillis())
            if (speechQueue.activeRequest() == null && _state.value == TtsEngineState.SPEAKING) {
                _state.value = TtsEngineState.READY
            }
            executeActionLocked(engine, action)
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
