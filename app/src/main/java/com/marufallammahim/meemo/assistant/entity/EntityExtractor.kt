package com.marufallammahim.meemo.assistant.entity

import com.marufallammahim.meemo.assistant.command.NormalizedCommand
import com.marufallammahim.meemo.assistant.intent.MeemoIntent
import javax.inject.Inject
import javax.inject.Singleton

enum class VolumeDirection {
    UP, DOWN, MUTE, UNMUTE
}

enum class SettingsTarget {
    MAIN_SETTINGS,
    WIFI_SETTINGS,
    BLUETOOTH_SETTINGS,
    DISPLAY_SETTINGS,
    SOUND_SETTINGS,
    NOTIFICATION_LISTENER_SETTINGS,
    ACCESSIBILITY_SETTINGS,
    APPLICATION_DETAILS_SETTINGS
}

sealed interface ExtractedEntity {
    data object None : ExtractedEntity
    data class AppQuery(val appName: String) : ExtractedEntity
    data class VolumePercentage(val levelPercent: Int, val streamName: String = "MUSIC") : ExtractedEntity
    data class VolumeAdjustment(val direction: VolumeDirection) : ExtractedEntity
    data class SettingsDestination(val target: SettingsTarget, val isDirectToggleAttempt: Boolean) : ExtractedEntity
    data class AliasDefinition(val triggerPhrase: String, val targetAppName: String) : ExtractedEntity
    data class AutomationTrigger(val routineName: String) : ExtractedEntity
    data class RootOperationTarget(val policyCommandId: String, val parameter: String? = null) : ExtractedEntity
}

/**
 * Extracts and validates typed entities for each recognized MeemoIntent.
 */
@Singleton
class EntityExtractor @Inject constructor() {

    fun extract(command: NormalizedCommand, intent: MeemoIntent): ExtractedEntity {
        val text = command.normalizedText.trim()
        val lower = text.lowercase()

        return when (intent) {
            MeemoIntent.OPEN_APPLICATION -> extractAppQuery(text)
            MeemoIntent.SET_VOLUME -> extractVolumePercentage(lower)
            MeemoIntent.ADJUST_VOLUME -> extractVolumeAdjustment(lower)
            MeemoIntent.OPEN_SETTINGS -> extractSettingsTarget(lower)
            MeemoIntent.CREATE_ALIAS -> extractAliasDefinition(text)
            MeemoIntent.RUN_AUTOMATION -> extractAutomationName(text)
            MeemoIntent.ROOT_SYSTEM_DIAGNOSTIC -> extractRootTarget(lower)
            else -> ExtractedEntity.None
        }
    }

    private fun extractAppQuery(text: String): ExtractedEntity {
        val englishMatch = Regex("(?i)^(?:please\\s+)?(?:open|launch|start)\\s+(?:my\\s+|the\\s+)?(.+)$").find(text)
        if (englishMatch != null) {
            val candidate = englishMatch.groupValues[1].trim()
            if (candidate.isNotBlank()) return ExtractedEntity.AppQuery(candidate)
        }

        val bengaliMatch = Regex("^(.+?)\\s+(?:অ্যাপ\\s+)?(?:খোলো|চালু করো|ওপেন করো|kholo|open koro|chalu koro)$", RegexOption.IGNORE_CASE).find(text)
        if (bengaliMatch != null) {
            val candidate = bengaliMatch.groupValues[1].trim()
            if (candidate.isNotBlank()) return ExtractedEntity.AppQuery(candidate)
        }

        return ExtractedEntity.AppQuery(text)
    }

    private fun extractVolumePercentage(lower: String): ExtractedEntity {
        val numberMatch = Regex("(\\d{1,3})").find(lower)
        val rawValue = numberMatch?.groupValues?.get(1)?.toIntOrNull() ?: 50
        val clamped = rawValue.coerceIn(0, 100)
        val stream = when {
            lower.contains("ring") -> "RING"
            lower.contains("alarm") -> "ALARM"
            else -> "MUSIC"
        }
        return ExtractedEntity.VolumePercentage(levelPercent = clamped, streamName = stream)
    }

    private fun extractVolumeAdjustment(lower: String): ExtractedEntity {
        val direction = when {
            lower.contains("unmute") || lower.contains("আনমিউট") -> VolumeDirection.UNMUTE
            lower.contains("mute") || lower.contains("মিউট") -> VolumeDirection.MUTE
            lower.contains("down") || lower.contains("decrease") || lower.contains("lower") ||
                lower.contains("কমাও") || lower.contains("komao") -> VolumeDirection.DOWN
            else -> VolumeDirection.UP
        }
        return ExtractedEntity.VolumeAdjustment(direction)
    }

    private fun extractSettingsTarget(lower: String): ExtractedEntity {
        val isDirectToggle = lower.startsWith("turn on") || lower.startsWith("turn off") ||
            lower.startsWith("enable") || lower.startsWith("disable") ||
            lower.contains("চালু করো") || lower.contains("বন্ধ করো")

        val target = when {
            lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("ওয়াইফাই") ->
                SettingsTarget.WIFI_SETTINGS
            lower.contains("bluetooth") || lower.contains("ব্লুটুথ") ->
                SettingsTarget.BLUETOOTH_SETTINGS
            lower.contains("notification") ->
                SettingsTarget.NOTIFICATION_LISTENER_SETTINGS
            lower.contains("accessibility") || lower.contains("অ্যাক্সেসিবিলিটি") ->
                SettingsTarget.ACCESSIBILITY_SETTINGS
            lower.contains("display") || lower.contains("ডিসপ্লে") ->
                SettingsTarget.DISPLAY_SETTINGS
            lower.contains("sound") || lower.contains("সাউন্ড") ->
                SettingsTarget.SOUND_SETTINGS
            else -> SettingsTarget.MAIN_SETTINGS
        }
        return ExtractedEntity.SettingsDestination(target = target, isDirectToggleAttempt = isDirectToggle)
    }

    private fun extractAliasDefinition(text: String): ExtractedEntity {
        val enRegex = Regex("(?i)^(?:whenever i say|when i say|save alias|create alias)\\s+(.+?)\\s+(?:open|run|launch)\\s+(.+)$")
        val enMatch = enRegex.find(text)
        if (enMatch != null) {
            return ExtractedEntity.AliasDefinition(
                triggerPhrase = enMatch.groupValues[1].trim().removeSurrounding("\""),
                targetAppName = enMatch.groupValues[2].trim().removeSurrounding("\"")
            )
        }
        val bnRegex = Regex("^যখন আমি বলব\\s+(.+?)\\s+তখন\\s+(.+?)\\s+(?:খোলো|চালু করো)$")
        val bnMatch = bnRegex.find(text)
        if (bnMatch != null) {
            return ExtractedEntity.AliasDefinition(
                triggerPhrase = bnMatch.groupValues[1].trim(),
                targetAppName = bnMatch.groupValues[2].trim()
            )
        }
        return ExtractedEntity.None
    }

    private fun extractAutomationName(text: String): ExtractedEntity {
        val enMatch = Regex("(?i)^(?:run|execute|start)\\s+(?:routine|automation)\\s+(.+)$").find(text)
        if (enMatch != null) {
            return ExtractedEntity.AutomationTrigger(enMatch.groupValues[1].trim())
        }
        val bnMatch = Regex("^রুটিন\\s+চালু\\s+করো\\s+(.+)$").find(text)
        if (bnMatch != null) {
            return ExtractedEntity.AutomationTrigger(bnMatch.groupValues[1].trim())
        }
        return ExtractedEntity.AutomationTrigger(text)
    }

    private fun extractRootTarget(lower: String): ExtractedEntity {
        return when {
            lower.contains("thermal") ->
                ExtractedEntity.RootOperationTarget("ROOT_THERMAL_STATUS")
            lower.contains("package") -> {
                val pkg = lower.substringAfter("package check").trim().ifBlank { "com.android.settings" }
                ExtractedEntity.RootOperationTarget("ROOT_PACKAGE_PATH_CHECK", pkg)
            }
            else ->
                ExtractedEntity.RootOperationTarget("ROOT_BATTERY_STATS_DUMP")
        }
    }
}
