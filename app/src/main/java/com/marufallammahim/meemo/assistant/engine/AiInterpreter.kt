package com.marufallammahim.meemo.assistant.engine

import com.marufallammahim.meemo.assistant.command.NormalizedCommand
import com.marufallammahim.meemo.assistant.entity.EntityExtractor
import com.marufallammahim.meemo.assistant.entity.ExtractedEntity
import com.marufallammahim.meemo.assistant.intent.MeemoIntent
import com.marufallammahim.meemo.core.common.DispatcherProvider
import com.marufallammahim.meemo.core.result.ResultWrapper
import com.marufallammahim.meemo.data.remote.OpenAiCompatibleClient
import com.marufallammahim.meemo.domain.validation.CommandSafetyValidator
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AiResolvedCommand(
    val intent: MeemoIntent,
    val entity: ExtractedEntity,
    val confidence: Float
)

interface AiInterpreter {
    suspend fun interpret(
        command: NormalizedCommand,
        endpointUrl: String,
        modelName: String
    ): ResultWrapper<AiResolvedCommand>
}

@Singleton
class DefaultAiInterpreter @Inject constructor(
    private val client: OpenAiCompatibleClient,
    private val safetyValidator: CommandSafetyValidator,
    private val entityExtractor: EntityExtractor,
    private val dispatchers: DispatcherProvider
) : AiInterpreter {

    override suspend fun interpret(
        command: NormalizedCommand,
        endpointUrl: String,
        modelName: String
    ): ResultWrapper<AiResolvedCommand> = withContext(dispatchers.io) {
        when (val rawResult = client.interpretCommand(endpointUrl, modelName, command.normalizedText)) {
            is ResultWrapper.Failure -> rawResult
            is ResultWrapper.Success -> {
                val structured = rawResult.data
                when (val validatedIntent = safetyValidator.validateAiInterpretation(structured)) {
                    is ResultWrapper.Failure -> validatedIntent
                    is ResultWrapper.Success -> {
                        val intent = validatedIntent.data
                        val syntheticCommand = if (!structured.entityValue.isNullOrBlank()) {
                            command.copy(normalizedText = structured.entityValue)
                        } else {
                            command
                        }
                        val entity = entityExtractor.extract(syntheticCommand, intent)
                        ResultWrapper.Success(
                            AiResolvedCommand(
                                intent = intent,
                                entity = entity,
                                confidence = structured.confidence
                            )
                        )
                    }
                }
            }
        }
    }
}
