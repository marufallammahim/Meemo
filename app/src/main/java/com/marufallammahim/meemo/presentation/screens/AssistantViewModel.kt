package com.marufallammahim.meemo.presentation.screens

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.assistant.command.CommandInputSource
import com.marufallammahim.meemo.assistant.engine.AssistantExecutionState
import com.marufallammahim.meemo.core.error.MeemoError
import com.marufallammahim.meemo.domain.model.ConversationEntryModel
import com.marufallammahim.meemo.domain.model.InstalledAppInfo
import com.marufallammahim.meemo.domain.model.UserPreferences
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.domain.usecase.ProcessCommandUseCase
import com.marufallammahim.meemo.settings.SettingsCoordinator
import com.marufallammahim.meemo.tts.TtsController
import com.marufallammahim.meemo.tts.TtsEngineState
import com.marufallammahim.meemo.voice.VoiceController
import com.marufallammahim.meemo.voice.VoiceRecognitionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AssistantHomeUiState(
    val executionState: AssistantExecutionState = AssistantExecutionState.IDLE,
    val inputDraft: String = "",
    val livePartialTranscript: String = "",
    val isTtsSpeaking: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val recentMessages: List<ConversationEntryModel> = emptyList(),
    val ambiguousCandidates: List<InstalledAppInfo> = emptyList(),
    val ambiguousQuery: String = "",
    val pendingConfirmationAction: MeemoAction? = null,
    val pendingConfirmationPrompt: String = "",
    val activeError: MeemoError? = null,
    val quickSuggestions: List<String> = DEFAULT_SUGGESTIONS
) {
    companion object {
        val DEFAULT_SUGGESTIONS = listOf(
            "Turn on the flashlight",
            "What is my battery status?",
            "Open my browser",
            "ফ্ল্যাশলাইট চালু করো",
            "ব্যাটারি কত?",
            "Device info",
            "Set volume to 50 percent"
        )
    }
}

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val processCommandUseCase: ProcessCommandUseCase,
    private val repository: MeemoRepository,
    private val voiceController: VoiceController,
    private val ttsController: TtsController,
    private val settingsCoordinator: SettingsCoordinator
) : ViewModel() {

    private val localState = MutableStateFlow(AssistantHomeUiState())

    val uiState: StateFlow<AssistantHomeUiState> = combine(
        localState,
        repository.userPreferencesFlow,
        repository.observeRecentMessages(limit = 40),
        voiceController.recognitionState,
        ttsController.ttsState
    ) { local, prefs, messages, voiceState, ttsState ->
        local.copy(
            preferences = prefs,
            recentMessages = messages,
            isTtsSpeaking = ttsState == TtsEngineState.SPEAKING,
            livePartialTranscript = (voiceState as? VoiceRecognitionState.PartialTranscript)?.text.orEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AssistantHomeUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
        viewModelScope.launch {
            voiceController.recognitionState.collect { vState ->
                when (vState) {
                    is VoiceRecognitionState.Listening -> {
                        localState.update { it.copy(executionState = AssistantExecutionState.LISTENING, activeError = null) }
                    }
                    is VoiceRecognitionState.PartialTranscript -> {
                        localState.update { it.copy(executionState = AssistantExecutionState.TRANSCRIBING) }
                    }
                    is VoiceRecognitionState.FinalTranscript -> {
                        submitCommand(vState.text, CommandInputSource.VOICE_MANUAL)
                    }
                    is VoiceRecognitionState.Error -> {
                        localState.update {
                            it.copy(
                                executionState = AssistantExecutionState.FAILED,
                                activeError = vState.error
                            )
                        }
                    }
                    VoiceRecognitionState.Idle -> Unit
                }
            }
        }
    }

    fun onInputDraftChanged(text: String) {
        localState.update { it.copy(inputDraft = text) }
    }

    fun submitDraftCommand() {
        val text = localState.value.inputDraft.trim()
        localState.update { it.copy(inputDraft = "") }
        submitCommand(text, CommandInputSource.TEXT)
    }

    fun submitCommand(
        rawCommand: String,
        source: CommandInputSource = CommandInputSource.TEXT
    ) {
        viewModelScope.launch {
            localState.update {
                it.copy(
                    activeError = null,
                    ambiguousCandidates = emptyList(),
                    pendingConfirmationAction = null
                )
            }
            val outcome = processCommandUseCase(
                rawCommand = rawCommand,
                source = source,
                onStateChanged = { stepState ->
                    localState.update { current -> current.copy(executionState = stepState) }
                }
            )
            handleTurnOutcome(outcome.finalState, outcome.actionResult, outcome.response.spokenText, outcome.response.languageTag)
        }
    }

    fun onSelectAmbiguousCandidate(app: InstalledAppInfo) {
        viewModelScope.launch {
            localState.update { it.copy(ambiguousCandidates = emptyList(), ambiguousQuery = "") }
            val outcome = processCommandUseCase.confirmAction(
                action = MeemoAction.OpenApplication(app.packageName, app.appLabel),
                onStateChanged = { stepState ->
                    localState.update { current -> current.copy(executionState = stepState) }
                }
            )
            handleTurnOutcome(outcome.finalState, outcome.actionResult, outcome.response.spokenText, outcome.response.languageTag)
        }
    }

    fun onConfirmPendingAction(action: MeemoAction) {
        viewModelScope.launch {
            localState.update { it.copy(pendingConfirmationAction = null, pendingConfirmationPrompt = "") }
            val outcome = processCommandUseCase.confirmAction(
                action = action,
                onStateChanged = { stepState ->
                    localState.update { current -> current.copy(executionState = stepState) }
                }
            )
            handleTurnOutcome(outcome.finalState, outcome.actionResult, outcome.response.spokenText, outcome.response.languageTag)
        }
    }

    fun cancelActiveOperation() {
        voiceController.cancelVoiceCapture()
        ttsController.stopSpeaking()
        localState.update {
            it.copy(
                executionState = AssistantExecutionState.CANCELLED,
                ambiguousCandidates = emptyList(),
                pendingConfirmationAction = null,
                activeError = null
            )
        }
    }

    fun toggleVoiceListening() {
        val current = localState.value.executionState
        if (current == AssistantExecutionState.LISTENING) {
            voiceController.stopVoiceCapture()
        } else {
            ttsController.stopSpeaking()
            val localeTag = uiState.value.preferences.languageMode.bcp47Tag
            voiceController.startVoiceCapture(localeTag)
        }
    }

    fun stopSpeaking() {
        ttsController.stopSpeaking()
    }

    fun dismissError() {
        localState.update { it.copy(activeError = null, executionState = AssistantExecutionState.IDLE) }
    }

    fun executeRecoveryForError(error: MeemoError) {
        when (error.recoveryAction) {
            com.marufallammahim.meemo.core.error.RecoveryActionType.OPEN_NOTIFICATION_LISTENER_SETTINGS ->
                settingsCoordinator.openNotificationListenerSettings()
            com.marufallammahim.meemo.core.error.RecoveryActionType.OPEN_ACCESSIBILITY_SETTINGS ->
                settingsCoordinator.openAccessibilitySettings()
            com.marufallammahim.meemo.core.error.RecoveryActionType.OPEN_APP_SETTINGS,
            com.marufallammahim.meemo.core.error.RecoveryActionType.OPEN_SYSTEM_SETTINGS ->
                settingsCoordinator.openAppDetailsSettings()
            else -> dismissError()
        }
    }

    private fun handleTurnOutcome(
        finalState: AssistantExecutionState,
        actionResult: ActionResult,
        spokenText: String,
        languageTag: String
    ) {
        val prefs = uiState.value.preferences
        when (actionResult) {
            is ActionResult.Ambiguous -> {
                localState.update {
                    it.copy(
                        executionState = AssistantExecutionState.AWAITING_CONFIRMATION,
                        ambiguousQuery = actionResult.query,
                        ambiguousCandidates = actionResult.candidateApps
                    )
                }
            }
            is ActionResult.RequiresConfirmation -> {
                localState.update {
                    it.copy(
                        executionState = AssistantExecutionState.AWAITING_CONFIRMATION,
                        pendingConfirmationAction = actionResult.pendingAction,
                        pendingConfirmationPrompt = actionResult.promptEn
                    )
                }
            }
            is ActionResult.PermissionRequired -> {
                localState.update {
                    it.copy(
                        executionState = finalState,
                        activeError = actionResult.error
                    )
                }
            }
            is ActionResult.Unavailable -> {
                localState.update {
                    it.copy(
                        executionState = finalState,
                        activeError = actionResult.error
                    )
                }
            }
            is ActionResult.Failed -> {
                localState.update {
                    it.copy(
                        executionState = finalState,
                        activeError = actionResult.error
                    )
                }
            }
            else -> {
                localState.update { it.copy(executionState = finalState) }
            }
        }

        if (spokenText.isNotBlank() && prefs.ttsEnabled) {
            ttsController.speakResponse(
                text = spokenText,
                languageTag = languageTag,
                speechRate = prefs.ttsSpeechRate,
                ttsEnabled = prefs.ttsEnabled
            )
        }
    }
}
