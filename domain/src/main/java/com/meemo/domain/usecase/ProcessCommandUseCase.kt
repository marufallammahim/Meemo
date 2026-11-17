package com.meemo.domain.usecase

import com.meemo.core.model.ActionResult
import com.meemo.core.model.Command
import com.meemo.core.model.InputSource
import com.meemo.domain.command.CommandActionExecutor
import com.meemo.domain.command.CommandParserPort

data class ProcessCommandResult(val command: Command, val actionResult: ActionResult)

class ProcessCommandUseCase(
    private val parser: CommandParserPort,
    private val actionExecutor: CommandActionExecutor
) {
    suspend operator fun invoke(rawText: String, source: InputSource = InputSource.TEXT): ProcessCommandResult {
        val command = parser.parse(rawText, source)
        val result = when {
            command.confidence >= EXECUTION_CONFIDENCE -> actionExecutor.execute(command)
            command.confidence >= CONFIRMATION_CONFIDENCE ->
                ActionResult.NeedsConfirmation("Did you mean “${command.normalizedText}”?", command.intent)
            else -> ActionResult.Unsupported(
                "I did not understand that command. Try “open browser”, “battery”, or “open Wi-Fi settings”."
            )
        }
        return ProcessCommandResult(command, result)
    }

    private companion object {
        const val EXECUTION_CONFIDENCE = 0.8f
        const val CONFIRMATION_CONFIDENCE = 0.5f
    }
}
