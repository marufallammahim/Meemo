package com.marufallammahim.meemo.assistant.response

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.assistant.command.DetectedLanguage
import com.marufallammahim.meemo.domain.model.AppLanguageMode
import javax.inject.Inject
import javax.inject.Singleton

data class GeneratedAssistantResponse(
    val displayTitle: String,
    val displayBody: String,
    val spokenText: String,
    val languageTag: String,
    val isError: Boolean,
    val outcomeBadge: String
)

/**
 * Generates truthful, localized text and TTS output strictly derived from the actual ActionResult.
 */
@Singleton
class ResponseGenerator @Inject constructor() {

    fun generate(
        result: ActionResult,
        languageMode: AppLanguageMode,
        detectedLanguage: DetectedLanguage
    ): GeneratedAssistantResponse {
        val useBengali = when (languageMode) {
            AppLanguageMode.BENGALI -> true
            AppLanguageMode.ENGLISH -> false
            AppLanguageMode.BILINGUAL_AUTO -> detectedLanguage == DetectedLanguage.BENGALI
        }
        val langTag = if (useBengali) "bn-BD" else "en-US"

        return when (result) {
            is ActionResult.Success -> {
                val body = if (useBengali) result.summaryBn else result.summaryEn
                GeneratedAssistantResponse(
                    displayTitle = if (useBengali) "সফলভাবে সম্পন্ন" else "Completed",
                    displayBody = body,
                    spokenText = body,
                    languageTag = langTag,
                    isError = false,
                    outcomeBadge = if (result.verifiedBySystem) "VERIFIED" else "COMPLETED"
                )
            }
            is ActionResult.PartialSuccess -> {
                val summary = if (useBengali) result.summaryBn else result.summaryEn
                val reason = if (useBengali) result.reasonBn else result.reasonEn
                GeneratedAssistantResponse(
                    displayTitle = if (useBengali) "ব্যবহারকারীর পদক্ষেপ প্রয়োজন" else "Action Needed in Settings",
                    displayBody = "$summary ($reason)",
                    spokenText = summary,
                    languageTag = langTag,
                    isError = false,
                    outcomeBadge = "SETTINGS_OPENED"
                )
            }
            is ActionResult.PermissionRequired -> {
                val title = if (useBengali) result.error.titleBn else result.error.titleEn
                val msg = if (useBengali) result.error.messageBn else result.error.messageEn
                GeneratedAssistantResponse(
                    displayTitle = title,
                    displayBody = msg,
                    spokenText = msg,
                    languageTag = langTag,
                    isError = true,
                    outcomeBadge = "PERMISSION_REQUIRED"
                )
            }
            is ActionResult.Ambiguous -> {
                val names = result.candidateApps.joinToString(", ") { it.appLabel }
                val bodyEn = "Multiple apps match '${result.query}': $names. Please tap the one you want."
                val bodyBn = "'${result.query}' নামের একাধিক অ্যাপ পাওয়া গেছে: $names। অনুগ্রহ করে একটি বেছে নিন।"
                val text = if (useBengali) bodyBn else bodyEn
                GeneratedAssistantResponse(
                    displayTitle = if (useBengali) "অ্যাপ নির্বাচন করুন" else "Choose Application",
                    displayBody = text,
                    spokenText = text,
                    languageTag = langTag,
                    isError = false,
                    outcomeBadge = "CHOOSE_OPTION"
                )
            }
            is ActionResult.RequiresConfirmation -> {
                val prompt = if (useBengali) result.promptBn else result.promptEn
                GeneratedAssistantResponse(
                    displayTitle = if (useBengali) "নিশ্চিতকরণ প্রয়োজন" else "Confirmation Required",
                    displayBody = prompt,
                    spokenText = prompt,
                    languageTag = langTag,
                    isError = false,
                    outcomeBadge = "AWAITING_CONFIRMATION"
                )
            }
            is ActionResult.Unavailable -> {
                val title = if (useBengali) result.error.titleBn else result.error.titleEn
                val msg = if (useBengali) result.error.messageBn else result.error.messageEn
                GeneratedAssistantResponse(
                    displayTitle = title,
                    displayBody = msg,
                    spokenText = msg,
                    languageTag = langTag,
                    isError = true,
                    outcomeBadge = "UNAVAILABLE"
                )
            }
            is ActionResult.Cancelled -> {
                val msg = if (useBengali) result.messageBn else result.messageEn
                GeneratedAssistantResponse(
                    displayTitle = if (useBengali) "বাতিল" else "Cancelled",
                    displayBody = msg,
                    spokenText = "",
                    languageTag = langTag,
                    isError = false,
                    outcomeBadge = "CANCELLED"
                )
            }
            is ActionResult.Failed -> {
                val title = if (useBengali) result.error.titleBn else result.error.titleEn
                val msg = if (useBengali) result.error.messageBn else result.error.messageEn
                GeneratedAssistantResponse(
                    displayTitle = title,
                    displayBody = msg,
                    spokenText = msg,
                    languageTag = langTag,
                    isError = true,
                    outcomeBadge = result.error.code
                )
            }
        }
    }
}
