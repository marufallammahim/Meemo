package com.marufallammahim.meemo.assistant.entity

import com.marufallammahim.meemo.assistant.intent.MeemoIntent
import com.marufallammahim.meemo.assistant.parser.CommandNormalizer
import com.marufallammahim.meemo.assistant.parser.LanguageResolver
import com.marufallammahim.meemo.core.utilities.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EntityExtractorTest {

    private lateinit var normalizer: CommandNormalizer
    private lateinit var extractor: EntityExtractor

    @Before
    fun setUp() {
        val time = object : TimeProvider {
            override fun nowMillis(): Long = 1000L
            override fun currentHourOfDay(): Int = 10
        }
        normalizer = CommandNormalizer(LanguageResolver(), time)
        extractor = EntityExtractor()
    }

    @Test
    fun `extracts application name from English and Bengali commands`() {
        val cmdEn = normalizer.normalize("Hello Meemo, open Telegram")
        val entityEn = extractor.extract(cmdEn, MeemoIntent.OPEN_APPLICATION) as ExtractedEntity.AppQuery
        assertEquals("Telegram", entityEn.appName)

        val cmdBn = normalizer.normalize("হ্যালো মিমো, বিকাশ অ্যাপ খোলো")
        val entityBn = extractor.extract(cmdBn, MeemoIntent.OPEN_APPLICATION) as ExtractedEntity.AppQuery
        assertEquals("বিকাশ", entityBn.appName)
    }

    @Test
    fun `extracts and clamps volume percentage`() {
        val cmd = normalizer.normalize("ভলিউম ৭৫ শতাংশে সেট করো")
        val entity = extractor.extract(cmd, MeemoIntent.SET_VOLUME) as ExtractedEntity.VolumePercentage
        assertEquals(75, entity.levelPercent)
    }

    @Test
    fun `extracts all supported volume adjustment directions`() {
        val cases = listOf(
            "volume up" to VolumeDirection.UP,
            "volume down" to VolumeDirection.DOWN,
            "mute" to VolumeDirection.MUTE,
            "unmute" to VolumeDirection.UNMUTE
        )

        cases.forEach { (input, expected) ->
            val command = normalizer.normalize(input)
            val entity = extractor.extract(command, MeemoIntent.ADJUST_VOLUME) as ExtractedEntity.VolumeAdjustment
            assertEquals(expected, entity.direction)
        }
    }

    @Test
    fun `extracts alias trigger and target app`() {
        val cmd = normalizer.normalize("Whenever I say work browser open Firefox")
        val entity = extractor.extract(cmd, MeemoIntent.CREATE_ALIAS) as ExtractedEntity.AliasDefinition
        assertEquals("work browser", entity.triggerPhrase)
        assertEquals("Firefox", entity.targetAppName)
    }

    @Test
    fun `extracts Bengali notification settings destination`() {
        val command = normalizer.normalize("নোটিফিকেশন সেটিংস খোলো")
        val entity = extractor.extract(command, MeemoIntent.OPEN_SETTINGS) as ExtractedEntity.SettingsDestination
        assertEquals(SettingsTarget.NOTIFICATION_LISTENER_SETTINGS, entity.target)
    }

    @Test
    fun `marks direct Wi-Fi toggle as settings fallback`() {
        val cmd = normalizer.normalize("Turn on Wi-Fi")
        val entity = extractor.extract(cmd, MeemoIntent.OPEN_SETTINGS) as ExtractedEntity.SettingsDestination
        assertEquals(SettingsTarget.WIFI_SETTINGS, entity.target)
        assertTrue(entity.isDirectToggleAttempt)
    }
}
