package com.marufallammahim.meemo.assistant.engine

import com.marufallammahim.meemo.actions.ActionDispatcher
import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.assistant.command.CommandInputSource
import com.marufallammahim.meemo.assistant.context.ConversationContextManager
import com.marufallammahim.meemo.assistant.entity.EntityExtractor
import com.marufallammahim.meemo.assistant.entity.ExtractedEntity
import com.marufallammahim.meemo.assistant.intent.DeterministicIntentMatcher
import com.marufallammahim.meemo.assistant.intent.MeemoIntent
import com.marufallammahim.meemo.assistant.parser.CommandNormalizer
import com.marufallammahim.meemo.assistant.planning.ActionPlanOutcome
import com.marufallammahim.meemo.assistant.planning.ActionPlanner
import com.marufallammahim.meemo.assistant.response.GeneratedAssistantResponse
import com.marufallammahim.meemo.assistant.response.ResponseGenerator
import com.marufallammahim.meemo.capabilities.CapabilityCheckResult
import com.marufallammahim.meemo.capabilities.CapabilityManager
import com.marufallammahim.meemo.core.error.MeemoError
import com.marufallammahim.meemo.core.result.ResultWrapper
import com.marufallammahim.meemo.core.utilities.TimeProvider
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import com.marufallammahim.meemo.domain.validation.CommandSafetyValidator
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

enum class AssistantExecutionState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    PROCESSING,
    CHECKING_CAPABILITIES,
    AWAITING_CONFIRMATION,
    EXECUTING,
    VERIFYING,
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    CANCELLED,
    UNAVAILABLE
}

data class PipelineTurnOutcome(
    val finalState: AssistantExecutionState,
    val actionResult: ActionResult,
    val response: GeneratedAssistantResponse,
    val matchedIntent: MeemoIntent,
    val confidence: Float
)

/**
 * Core deterministic + hybrid command pipeline for Meemo.
 * All voice, text, alias, and AI-interpreted inputs pass through this exact validated pipeline.
 */
@Singleton
class AssistantEngine @Inject constructor(
    private val normalizer: CommandNormalizer,
    private val intentMatcher: DeterministicIntentMatcher,
    private val entityExtractor: EntityExtractor,
    private val capabilityManager: CapabilityManager,
    private val safetyValidator: CommandSafetyValidator,
    private val actionPlanner: ActionPlanner,
    private val actionDispatcher: ActionDispatcher,
    private val responseGenerator: ResponseGenerator,
    private val aiInterpreter: AiInterpreter,
    private val contextManager: ConversationContextManager,
    private val repository: MeemoRepository,
    private val timeProvider: TimeProvider
) {
    suspend fun processInput(
        rawInput: String,
        source: CommandInputSource = CommandInputSource.TEXT,
        onStateChanged: (AssistantExecutionState) -> Unit = {}
    ): PipelineTurnOutcome {
        val startMs = timeProvider.nowMillis()
        onStateChanged(AssistantExecutionState.PROCESSING)

        val prefs = repository.userPreferencesFlow.first()
        val normalized = normalizer.normalize(rawInput, source)

        // 1. Empty command check
        if (normalized.normalizedText.isBlank()) {
            val failResult = ActionResult.Failed(MeemoError.EmptyCommand)
            val resp = responseGenerator.generate(failResult, prefs.languageMode, normalized.detectedLanguage)
            onStateChanged(AssistantExecutionState.FAILED)
            return PipelineTurnOutcome(
                finalState = AssistantExecutionState.FAILED,
                actionResult = failResult,
                response = resp,
                matchedIntent = MeemoIntent.UNKNOWN,
                confidence = 0f
            )
        }

        // 2. Duplicate submission debounce guard
        if (contextManager.isDuplicateSubmission(normalized.normalizedText)) {
            val cancelResult = ActionResult.Cancelled(
                messageEn = "Duplicate command ignored.",
                messageBn = "একই কমান্ড পুনরায় পাঠানো এড়ানো হয়েছে।"
            )
            val resp = responseGenerator.generate(cancelResult, prefs.languageMode, normalized.detectedLanguage)
            onStateChanged(AssistantExecutionState.CANCELLED)
            return PipelineTurnOutcome(
                finalState = AssistantExecutionState.CANCELLED,
                actionResult = cancelResult,
                response = resp,
                matchedIntent = MeemoIntent.CANCEL_COMMAND,
                confidence = 1f
            )
        }

        // 3. Check user-defined custom aliases ("open my work browser" or exact alias trigger)
        val cleanedTrigger = normalized.normalizedText.lowercase()
            .removePrefix("open my ")
            .removePrefix("open ")
            .trim()
        val matchedAlias = repository.findAliasByTrigger(cleanedTrigger)
            ?: repository.findAliasByTrigger(normalized.normalizedText)

        var resolvedIntent: MeemoIntent
        var resolvedEntity: ExtractedEntity
        var confidence: Float

        if (matchedAlias != null) {
            resolvedIntent = MeemoIntent.fromNameOrUnknown(matchedAlias.targetIntent)
            resolvedEntity = ExtractedEntity.AppQuery(matchedAlias.targetEntity)
            confidence = 0.98f
        } else {
            // 4. Deterministic intent matching
            val match = intentMatcher.matchIntent(normalized)
            resolvedIntent = match.intent
            confidence = match.confidence
            resolvedEntity = entityExtractor.extract(normalized, resolvedIntent)

            // 5. Optional AI fallback if deterministic matcher returned UNKNOWN and user enabled AI
            if (resolvedIntent == MeemoIntent.UNKNOWN && prefs.aiFallbackEnabled) {
                when (val aiRes = aiInterpreter.interpret(normalized, prefs.aiEndpointUrl, prefs.aiModelName)) {
                    is ResultWrapper.Success -> {
                        resolvedIntent = aiRes.data.intent
                        resolvedEntity = aiRes.data.entity
                        confidence = aiRes.data.confidence
                    }
                    is ResultWrapper.Failure -> {
                        // Fall back cleanly to deterministic unrecognized command
                    }
                }
            }
        }

        if (resolvedIntent == MeemoIntent.UNKNOWN) {
            val failResult = ActionResult.Failed(MeemoError.UnrecognizedCommand(normalized.rawText))
            return finalizeTurn(
                startMs = startMs,
                rawInput = rawInput,
                normalizedText = normalized.normalizedText,
                intent = MeemoIntent.UNKNOWN,
                entityText = null,
                confidence = confidence,
                result = failResult,
                languageMode = prefs.languageMode,
                detectedLanguage = normalized.detectedLanguage,
                onStateChanged = onStateChanged
            )
        }

        // 6. Capability Verification
        onStateChanged(AssistantExecutionState.CHECKING_CAPABILITIES)
        when (val capCheck = capabilityManager.checkCapability(resolvedIntent.requiresHardwareCapability)) {
            is CapabilityCheckResult.Ready -> Unit
            is CapabilityCheckResult.PermissionMissing -> {
                return finalizeTurn(
                    startMs = startMs,
                    rawInput = rawInput,
                    normalizedText = normalized.normalizedText,
                    intent = resolvedIntent,
                    entityText = resolvedEntity.toString(),
                    confidence = confidence,
                    result = ActionResult.PermissionRequired(capCheck.error),
                    languageMode = prefs.languageMode,
                    detectedLanguage = normalized.detectedLanguage,
                    onStateChanged = onStateChanged
                )
            }
            is CapabilityCheckResult.HardwareAbsent -> {
                return finalizeTurn(
                    startMs = startMs,
                    rawInput = rawInput,
                    normalizedText = normalized.normalizedText,
                    intent = resolvedIntent,
                    entityText = resolvedEntity.toString(),
                    confidence = confidence,
                    result = ActionResult.Unavailable(capCheck.error),
                    languageMode = prefs.languageMode,
                    detectedLanguage = normalized.detectedLanguage,
                    onStateChanged = onStateChanged
                )
            }
            is CapabilityCheckResult.RestrictedByPlatform -> {
                return finalizeTurn(
                    startMs = startMs,
                    rawInput = rawInput,
                    normalizedText = normalized.normalizedText,
                    intent = resolvedIntent,
                    entityText = resolvedEntity.toString(),
                    confidence = confidence,
                    result = ActionResult.Unavailable(capCheck.error),
                    languageMode = prefs.languageMode,
                    detectedLanguage = normalized.detectedLanguage,
                    onStateChanged = onStateChanged
                )
            }
            is CapabilityCheckResult.RootUnavailable -> {
                return finalizeTurn(
                    startMs = startMs,
                    rawInput = rawInput,
                    normalizedText = normalized.normalizedText,
                    intent = resolvedIntent,
                    entityText = resolvedEntity.toString(),
                    confidence = confidence,
                    result = ActionResult.Unavailable(capCheck.error),
                    languageMode = prefs.languageMode,
                    detectedLanguage = normalized.detectedLanguage,
                    onStateChanged = onStateChanged
                )
            }
        }

        // 7. Action Planning
        val planOutcome = actionPlanner.plan(resolvedIntent, resolvedEntity, prefs.preferredBrowserPackage)
        val plannedAction = when (planOutcome) {
            is ActionPlanOutcome.ImmediateResult -> {
                return finalizeTurn(
                    startMs = startMs,
                    rawInput = rawInput,
                    normalizedText = normalized.normalizedText,
                    intent = resolvedIntent,
                    entityText = resolvedEntity.toString(),
                    confidence = confidence,
                    result = planOutcome.result,
                    languageMode = prefs.languageMode,
                    detectedLanguage = normalized.detectedLanguage,
                    onStateChanged = onStateChanged
                )
            }
            is ActionPlanOutcome.Planned -> planOutcome.action
        }

        // 8. Safety Validation
        when (val validation = safetyValidator.validateAction(plannedAction)) {
            is ResultWrapper.Failure -> {
                return finalizeTurn(
                    startMs = startMs,
                    rawInput = rawInput,
                    normalizedText = normalized.normalizedText,
                    intent = resolvedIntent,
                    entityText = resolvedEntity.toString(),
                    confidence = confidence,
                    result = ActionResult.Failed(validation.error),
                    languageMode = prefs.languageMode,
                    detectedLanguage = normalized.detectedLanguage,
                    onStateChanged = onStateChanged
                )
            }
            is ResultWrapper.Success -> Unit
        }

        // 9. Confirmation Check for Sensitive / Root Actions
        if (plannedAction.requiresConfirmation) {
            val confirmResult = ActionResult.RequiresConfirmation(
                pendingAction = plannedAction,
                promptEn = "Confirm executing privileged action: ${resolvedIntent.name}?",
                promptBn = "আপনি কি এই সংবেদনশীল কমান্ডটি (${resolvedIntent.name}) কার্যকর করতে নিশ্চিত?"
            )
            return finalizeTurn(
                startMs = startMs,
                rawInput = rawInput,
                normalizedText = normalized.normalizedText,
                intent = resolvedIntent,
                entityText = resolvedEntity.toString(),
                confidence = confidence,
                result = confirmResult,
                languageMode = prefs.languageMode,
                detectedLanguage = normalized.detectedLanguage,
                onStateChanged = onStateChanged
            )
        }

        // 10. Execute & Verify
        return executeConfirmedAction(
            action = plannedAction,
            intent = resolvedIntent,
            rawInput = rawInput,
            normalizedText = normalized.normalizedText,
            confidence = confidence,
            startMs = startMs,
            onStateChanged = onStateChanged
        )
    }

    suspend fun executeConfirmedAction(
        action: MeemoAction,
        intent: MeemoIntent = MeemoIntent.OPEN_APPLICATION,
        rawInput: String = action.toString(),
        normalizedText: String = action.toString(),
        confidence: Float = 1.0f,
        startMs: Long = timeProvider.nowMillis(),
        onStateChanged: (AssistantExecutionState) -> Unit = {}
    ): PipelineTurnOutcome {
        val prefs = repository.userPreferencesFlow.first()
        val detectedLang = normalizer.normalize(rawInput).detectedLanguage

        onStateChanged(AssistantExecutionState.EXECUTING)
        val result = actionDispatcher.dispatch(action)
        onStateChanged(AssistantExecutionState.VERIFYING)

        return finalizeTurn(
            startMs = startMs,
            rawInput = rawInput,
            normalizedText = normalizedText,
            intent = intent,
            entityText = action.javaClass.simpleName,
            confidence = confidence,
            result = result,
            languageMode = prefs.languageMode,
            detectedLanguage = detectedLang,
            onStateChanged = onStateChanged
        )
    }

    private suspend fun finalizeTurn(
        startMs: Long,
        rawInput: String,
        normalizedText: String,
        intent: MeemoIntent,
        entityText: String?,
        confidence: Float,
        result: ActionResult,
        languageMode: com.marufallammahim.meemo.domain.model.AppLanguageMode,
        detectedLanguage: com.marufallammahim.meemo.assistant.command.DetectedLanguage,
        onStateChanged: (AssistantExecutionState) -> Unit
    ): PipelineTurnOutcome {
        val finalState = when (result) {
            is ActionResult.Success -> AssistantExecutionState.SUCCESS
            is ActionResult.PartialSuccess -> AssistantExecutionState.PARTIAL_SUCCESS
            is ActionResult.RequiresConfirmation, is ActionResult.Ambiguous -> AssistantExecutionState.AWAITING_CONFIRMATION
            is ActionResult.Unavailable -> AssistantExecutionState.UNAVAILABLE
            is ActionResult.Cancelled -> AssistantExecutionState.CANCELLED
            is ActionResult.PermissionRequired, is ActionResult.Failed -> AssistantExecutionState.FAILED
        }
        onStateChanged(finalState)

        val response = responseGenerator.generate(result, languageMode, detectedLanguage)
        val latency = (timeProvider.nowMillis() - startMs).coerceAtLeast(1L)

        repository.recordInteraction(
            sessionId = contextManager.currentSessionId(),
            rawInput = rawInput,
            normalizedInput = normalizedText,
            matchedIntent = intent.name,
            extractedEntity = entityText,
            confidence = confidence,
            outcomeCode = result.outcomeCode,
            assistantReply = response.displayBody,
            latencyMs = latency
        )

        return PipelineTurnOutcome(
            finalState = finalState,
            actionResult = result,
            response = response,
            matchedIntent = intent,
            confidence = confidence
        )
    }
}
