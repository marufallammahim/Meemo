package com.meemo.nlu

import com.meemo.core.model.CommandEntity
import com.meemo.core.model.SettingsPage
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityExtractorTest {
    @Test
    fun extractsAliasDefinition() {
        assertTrue(EntityExtractor().extract("whenever i say work open chrome")
            .contains(CommandEntity.AliasDefinition("work", "chrome")))
    }

    @Test
    fun extractsNumericAndWordNumbers() {
        val entities = EntityExtractor().extract("set volume to 50 percent and fifty")
        assertTrue(entities.contains(CommandEntity.NumberValue(50)))
    }

    @Test
    fun extractsAppName() {
        assertTrue(EntityExtractor().extract("open chrome").contains(CommandEntity.AppName("chrome")))
    }

    @Test
    fun extractsWifiSetting() {
        assertTrue(EntityExtractor().extract("open wifi settings").contains(CommandEntity.Setting(SettingsPage.WIFI)))
    }
}
