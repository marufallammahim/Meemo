package com.marufallammahim.meemo.domain.validation

import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.assistant.intent.MeemoIntent
import com.marufallammahim.meemo.core.error.MeemoError
import com.marufallammahim.meemo.core.result.ResultWrapper
import com.marufallammahim.meemo.data.remote.AiStructuredInterpretation
import com.marufallammahim.meemo.root.RootCommandPolicy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates both deterministic and AI-interpreted intents/entities against
 * prompt injection, shell metacharacters, and unauthorized privileged actions.
 */
@Singleton
class CommandSafetyValidator @Inject constructor(
    private val rootCommandPolicy: RootCommandPolicy
) {
    fun validateAiInterpretation(interpretation: AiStructuredInterpretation): ResultWrapper<MeemoIntent> {
        if (interpretation.confidence < MIN_AI_CONFIDENCE) {
            return ResultWrapper.Failure(
                MeemoError.UnrecognizedCommand("AI confidence too low (${interpretation.confidence})")
            )
        }
        val intent = MeemoIntent.fromNameOrUnknown(interpretation.intentName)
        if (intent == MeemoIntent.UNKNOWN) {
            return ResultWrapper.Failure(
                MeemoError.UnrecognizedCommand(interpretation.intentName)
            )
        }
        // AI is never permitted to trigger root diagnostics directly
        if (intent == MeemoIntent.ROOT_SYSTEM_DIAGNOSTIC) {
            return ResultWrapper.Failure(
                MeemoError.RootUnavailableOrRejected("AI cannot initiate root operations")
            )
        }
        val entity = interpretation.entityValue.orEmpty()
        if (SHELL_METACHAR_REGEX.containsMatchIn(entity)) {
            return ResultWrapper.Failure(
                MeemoError.InternalFailure("Rejected unsafe characters in AI entity")
            )
        }
        return ResultWrapper.Success(intent)
    }

    fun validateAction(action: MeemoAction): ResultWrapper<MeemoAction> {
        return when (action) {
            is MeemoAction.ExecuteApprovedRootDiagnostic -> {
                if (!rootCommandPolicy.isCommandAllowed(action.policyCommandId, action.parameter)) {
                    ResultWrapper.Failure(
                        MeemoError.RootUnavailableOrRejected("Unapproved root command or parameter: ${action.policyCommandId}")
                    )
                } else {
                    ResultWrapper.Success(action)
                }
            }
            is MeemoAction.SetVolume -> {
                if (action.levelPercent !in 0..100) {
                    ResultWrapper.Failure(MeemoError.InternalFailure("Volume out of range 0..100"))
                } else {
                    ResultWrapper.Success(action)
                }
            }
            else -> ResultWrapper.Success(action)
        }
    }

    companion object {
        const val MIN_AI_CONFIDENCE = 0.70f
        private val SHELL_METACHAR_REGEX = Regex("[;|&`$><\\n\\r]")
    }
}
