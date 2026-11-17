package com.meemo.core.model

sealed interface CommandEntity {
    data class AppName(val value: String) : CommandEntity
    data class NumberValue(val value: Int) : CommandEntity
    data class AliasDefinition(val phrase: String, val target: String) : CommandEntity
    data class Setting(val page: SettingsPage) : CommandEntity
}
