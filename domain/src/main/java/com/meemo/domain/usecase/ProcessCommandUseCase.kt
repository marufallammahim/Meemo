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
        val result = if (command.confidence >= 0.8f) {
            actionExecutor.execute(command)
        } else {
            ActionResult.Unsupported(
                if (command.confidence >= 0.5f) "Please rephrase or confirm what you meant."
                else "I did not understand that command. Try “open browser”, “battery”, or “open Wi-Fi settings”."
            )
        }
        return ProcessCommandResult(command, result)
    }
}
