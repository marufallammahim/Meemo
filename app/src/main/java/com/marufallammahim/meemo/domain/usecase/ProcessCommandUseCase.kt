package com.marufallammahim.meemo.domain.usecase

import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.assistant.command.CommandInputSource
import com.marufallammahim.meemo.assistant.engine.AssistantEngine
import com.marufallammahim.meemo.assistant.engine.AssistantExecutionState
import com.marufallammahim.meemo.assistant.engine.PipelineTurnOutcome
import javax.inject.Inject

class ProcessCommandUseCase @Inject constructor(
    private val assistantEngine: AssistantEngine
) {
    suspend operator fun invoke(
        rawCommand: String,
        source: CommandInputSource = CommandInputSource.TEXT,
        onStateChanged: (AssistantExecutionState) -> Unit = {}
    ): PipelineTurnOutcome {
        return assistantEngine.processInput(rawCommand, source, onStateChanged)
    }

    suspend fun confirmAction(
        action: MeemoAction,
        onStateChanged: (AssistantExecutionState) -> Unit = {}
    ): PipelineTurnOutcome {
        return assistantEngine.executeConfirmedAction(action = action, onStateChanged = onStateChanged)
    }
}
