package com.meemo.feature.assistant

import com.meemo.core.model.ActionResult

data class AssistantMessage(val id: Long, val text: String, val isUser: Boolean, val status: String? = null)

data class AssistantUiState(
    val input: String = "",
    val messages: List<AssistantMessage> = emptyList(),
    val isProcessing: Boolean = false
)

internal fun ActionResult.toAssistantText(): Pair<String, String> = when (this) {
    is ActionResult.Success -> message to "Success"
    is ActionResult.NeedsPermission -> message to "Needs permission"
    is ActionResult.NeedsUserAction -> message to "Needs your action"
    is ActionResult.NeedsConfirmation -> message to "Confirmation needed"
    is ActionResult.Unsupported -> reason to "Unsupported"
    is ActionResult.Failed -> message to "Could not complete"
}
