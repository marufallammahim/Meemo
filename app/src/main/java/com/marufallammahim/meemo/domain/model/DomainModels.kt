package com.marufallammahim.meemo.domain.model

import kotlinx.serialization.Serializable

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromName(value: String?): AppThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

enum class AppLanguageMode(val displayLabel: String, val bcp47Tag: String) {
    BILINGUAL_AUTO("Auto (English + বাংলা)", "en-US"),
    ENGLISH("English", "en-US"),
    BENGALI("বাংলা (Bengali)", "bn-BD");

    companion object {
        fun fromName(value: String?): AppLanguageMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BILINGUAL_AUTO
    }
}

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val languageMode: AppLanguageMode = AppLanguageMode.BILINGUAL_AUTO,
    val ttsEnabled: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val saveConversationHistory: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val notificationReadAloudEnabled: Boolean = false,
    val notificationAnnounceSenderOnly: Boolean = false,
    val allowedNotificationPackages: Set<String> = emptySet(),
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 23,
    val quietHoursEndHour: Int = 7,
    val aiFallbackEnabled: Boolean = false,
    val aiEndpointUrl: String = "https://api.openai.com/v1/chat/completions",
    val aiModelName: String = "gpt-4o-mini",
    val rootFeaturesEnabled: Boolean = false,
    val preferredBrowserPackage: String? = null
)

data class InstalledAppInfo(
    val packageName: String,
    val appLabel: String,
    val isBrowser: Boolean = false,
    val isLaunchable: Boolean = true
)

data class CustomAliasModel(
    val aliasId: String,
    val triggerPhrase: String,
    val targetIntent: String,
    val targetEntity: String,
    val requiresConfirmation: Boolean,
    val isEnabled: Boolean,
    val usageCount: Int
)

@Serializable
data class AutomationStepSpec(
    val intentName: String,
    val entityValue: String? = null
)

data class AutomationRuleModel(
    val ruleId: String,
    val ruleName: String,
    val triggerPhrase: String,
    val steps: List<AutomationStepSpec>,
    val isEnabled: Boolean,
    val requireConfirmation: Boolean
)

data class ConversationEntryModel(
    val messageId: String,
    val sessionId: String,
    val isUser: Boolean,
    val text: String,
    val intentName: String?,
    val statusBadge: String,
    val timestampMs: Long
)
