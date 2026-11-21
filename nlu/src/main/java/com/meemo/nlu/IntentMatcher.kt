package com.meemo.nlu

import com.meemo.core.model.CommandEntity
import com.meemo.core.model.MeemoIntent
import com.meemo.core.model.SettingsPage

data class IntentMatch(val intent: MeemoIntent, val confidence: Float)

class IntentMatcher {
    private val batteryPattern = Regex(
        "^(get battery|battery|battery status|battery level|battery percentage|check battery|show battery)$"
    )
    private val browserPattern = Regex(
        "^(open )?browser$|^open web browser$|^open internet browser$|^browse internet$"
    )
    private val setVolumePatterns = listOf(
        Regex("^(?:set|change) (?:the )?(?:(?:media|music) )?volume to [\\p{Nd}]{1,3}(?:\\s*(?:%|percent))?$"),
        Regex(
            "^(?:volume|sound) [\\p{Nd}]{1,3}(?:\\s*(?:%|percent))?" +
                "(?:\\s*(?:set koro|koro|rakho|সেট করো|করো|রাখো))?$"
        ),
        Regex(
            "^ভলিউম [\\p{Nd}]{1,3}(?:\\s*(?:%|শতাংশে?|পার্সেন্টে?))?" +
                "(?:\\s*(?:সেট করো|করো|রাখো))?$"
        )
    )
    private val volumeUpPatterns = listOf(
        Regex("^(?:turn )?volume up$"),
        Regex("^turn (?:the )?volume up$"),
        Regex("^(?:volume|sound) up$"),
        Regex("^(?:increase|raise) (?:the )?volume$"),
        Regex("^ভলিউম বাড়াও$"),
        Regex("^(?:volume|sound) (?:barao|বাড়াও)$")
    )
    private val volumeDownPatterns = listOf(
        Regex("^(?:turn )?volume down$"),
        Regex("^turn (?:the )?volume down$"),
        Regex("^(?:volume|sound) down$"),
        Regex("^(?:decrease|lower) (?:the )?volume$"),
        Regex("^ভলিউম কমাও$"),
        Regex("^(?:volume|sound) (?:komao|কমাও)$")
    )

    fun match(text: String, entities: List<CommandEntity> = emptyList()): IntentMatch {
        val value = text.trim()
        if (value.isEmpty()) return IntentMatch(MeemoIntent.Unknown(value), 0f)
        if (batteryPattern.matches(value)) {
            return IntentMatch(MeemoIntent.GetBattery, 0.98f)
        }
        if (browserPattern.matches(value)) {
            return IntentMatch(MeemoIntent.OpenBrowser, 0.98f)
        }
        if (setVolumePatterns.any { it.matches(value) }) {
            val percent = entities.filterIsInstance<CommandEntity.NumberValue>().firstOrNull()?.value
                ?: return IntentMatch(MeemoIntent.Unknown(value), 0f)
            return IntentMatch(MeemoIntent.SetVolume(percent = percent.coerceIn(0, 100), delta = null), 0.98f)
        }
        if (volumeUpPatterns.any { it.matches(value) }) {
            return IntentMatch(MeemoIntent.SetVolume(percent = null, delta = 1), 0.98f)
        }
        if (volumeDownPatterns.any { it.matches(value) }) {
            return IntentMatch(MeemoIntent.SetVolume(percent = null, delta = -1), 0.98f)
        }
        settingsMatch(value)?.let { return IntentMatch(it, 0.98f) }
        entities.filterIsInstance<CommandEntity.AliasDefinition>().firstOrNull()?.let {
            return IntentMatch(MeemoIntent.DefineAlias(it.phrase, it.target), 0.96f)
        }
        fuzzyMatch(value)?.let { return it }
        entities.filterIsInstance<CommandEntity.AppName>().firstOrNull()?.let {
            return IntentMatch(MeemoIntent.OpenApp(it.value), 0.91f)
        }
        return IntentMatch(MeemoIntent.Unknown(value), 0f)
    }

    private fun settingsMatch(value: String): MeemoIntent.OpenSettings? {
        val phrase = value.removePrefix("open ").removePrefix("show ").removePrefix("go to ")
            .removeSuffix(" settings").trim()
        val page = when (phrase) {
            "wifi", "network" -> SettingsPage.WIFI
            "bluetooth" -> SettingsPage.BLUETOOTH
            "display", "screen" -> SettingsPage.DISPLAY
            "sound" -> SettingsPage.SOUND
            "battery" -> SettingsPage.BATTERY
            "accessibility" -> SettingsPage.ACCESSIBILITY
            "notifications", "notification" -> SettingsPage.NOTIFICATIONS
            "settings", "main settings" -> SettingsPage.MAIN
            else -> return null
        }
        return MeemoIntent.OpenSettings(page)
    }

    private fun fuzzyMatch(value: String): IntentMatch? {
        val candidates = listOf(
            "battery" to MeemoIntent.GetBattery,
            "open browser" to MeemoIntent.OpenBrowser,
            "open wifi settings" to MeemoIntent.OpenSettings(SettingsPage.WIFI),
            "open bluetooth settings" to MeemoIntent.OpenSettings(SettingsPage.BLUETOOTH),
            "open display settings" to MeemoIntent.OpenSettings(SettingsPage.DISPLAY),
            "open sound settings" to MeemoIntent.OpenSettings(SettingsPage.SOUND)
        )
        val match = FuzzyMatcher().rank(value, candidates) { it.first }.firstOrNull() ?: return null
        if (match.score < 0.6f || match.score >= 1f) return null
        return IntentMatch(match.value.second, (0.6f + match.score * 0.3f).coerceAtMost(0.9f))
    }
}
