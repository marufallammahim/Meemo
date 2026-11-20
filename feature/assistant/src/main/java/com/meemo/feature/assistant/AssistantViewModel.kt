package com.meemo.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meemo.core.model.InputSource
import com.meemo.domain.usecase.ProcessCommandUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssistantViewModel(private val processCommand: ProcessCommandUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun sendCommand() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.isProcessing) return
        _uiState.update {
            it.copy(
                input = "",
                isProcessing = true,
                messages = it.messages + AssistantMessage(System.nanoTime(), text, true)
            )
        }
        viewModelScope.launch {
            try {
                val result = processCommand(text, InputSource.TEXT)
                val (message, status) = result.actionResult.toAssistantText()
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        messages = state.messages + AssistantMessage(System.nanoTime(), message, false, status)
                    )
                }
            } catch (cancelled: CancellationException) {
                // Preserve structured cancellation when the ViewModel is cleared.
                throw cancelled
            } catch (failure: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isProcessing = false,
                        messages = state.messages + AssistantMessage(
                            System.nanoTime(),
                            "Something went wrong while processing that command.",
                            false,
                            "Error"
                        )
                    )
                }
            }
        }
    }
}
