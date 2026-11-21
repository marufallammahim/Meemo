package com.meemo.nlu

import com.meemo.core.model.CommandEntity
import com.meemo.core.model.SettingsPage

class EntityExtractor {
    fun extract(text: String): List<CommandEntity> {
        val entities = mutableListOf<CommandEntity>()
        Regex("^whenever i say (.+) open (.+)$").matchEntire(text)?.let {
            entities += CommandEntity.AliasDefinition(it.groupValues[1].trim(), it.groupValues[2].trim())
        }
        Regex("^open (.+)$").matchEntire(text)?.let {
            val app = it.groupValues[1].trim()
            if (app !in setOf("browser", "web browser", "settings", "wifi settings", "bluetooth settings")) {
                entities += CommandEntity.AppName(app)
            }
        }
        DECIMAL_NUMBER_PATTERN.findAll(text).forEach { match ->
            parseDecimalNumber(match.groupValues[1])?.let { number ->
                entities += CommandEntity.NumberValue(number)
            }
        }
        val numberWords = mapOf(
            "zero" to 0, "ten" to 10, "twenty" to 20, "thirty" to 30, "forty" to 40,
            "fifty" to 50, "sixty" to 60, "seventy" to 70, "eighty" to 80,
            "ninety" to 90, "hundred" to 100
        )
        text.split(" ").forEach { word -> numberWords[word]?.let { entities += CommandEntity.NumberValue(it) } }
        val page = when {
            "wifi" in text -> SettingsPage.WIFI
            "bluetooth" in text -> SettingsPage.BLUETOOTH
            "display" in text -> SettingsPage.DISPLAY
            "battery" in text -> SettingsPage.BATTERY
            else -> null
        }
        page?.let { entities += CommandEntity.Setting(it) }
        return entities.distinct()
    }

    private fun parseDecimalNumber(value: String): Int? {
        var number = 0
        for (character in value) {
            val digit = Character.digit(character, 10)
            if (digit < 0) return null
            number = number * 10 + digit
        }
        return number
    }

    private companion object {
        val DECIMAL_NUMBER_PATTERN = Regex("(?<![\\p{L}\\p{N}])([\\p{Nd}]{1,3})(?![\\p{L}\\p{N}])")
    }
}
