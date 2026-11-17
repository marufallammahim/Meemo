package com.meemo.nlu

import com.meemo.core.model.MeemoIntent
import com.meemo.core.model.SettingsPage

data class IntentMatch(val intent: MeemoIntent, val confidence: Float)

class IntentMatcher {
    fun match(text: String): IntentMatch {
        val value = text.trim()
        if (value.isEmpty()) return IntentMatch(MeemoIntent.Unknown(value), 0f)
        if (Regex("^(get battery|battery|battery status|check battery|show battery)$").matches(value)) {
            return IntentMatch(MeemoIntent.GetBattery, 0.98f)
        }
        if (Regex("^(open )?browser$|^open web browser$|^browse internet$").matches(value)) {
            return IntentMatch(MeemoIntent.OpenBrowser, 0.98f)
        }
        settingsMatch(value)?.let { return IntentMatch(it, 0.98f) }
        Regex("^whenever i say (.+) open (.+)$").matchEntire(value)?.let {
            return IntentMatch(MeemoIntent.DefineAlias(it.groupValues[1].trim(), it.groupValues[2].trim()), 0.96f)
        }
        Regex("^open (.+)$").matchEntire(value)?.let {
            val query = it.groupValues[1].trim()
            if (query.isNotEmpty() && query != "settings") return IntentMatch(MeemoIntent.OpenApp(query), 0.91f)
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
}
