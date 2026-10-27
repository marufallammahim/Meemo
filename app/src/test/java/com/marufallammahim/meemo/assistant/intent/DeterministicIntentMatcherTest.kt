package com.marufallammahim.meemo.assistant.intent

import com.marufallammahim.meemo.assistant.parser.CommandNormalizer
import com.marufallammahim.meemo.assistant.parser.LanguageResolver
import com.marufallammahim.meemo.core.utilities.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeterministicIntentMatcherTest {

    private lateinit var normalizer: CommandNormalizer
    private lateinit var matcher: DeterministicIntentMatcher

    @Before
    fun setUp() {
        val time = object : TimeProvider {
            override fun nowMillis(): Long = 1000L
            override fun currentHourOfDay(): Int = 10
        }
        normalizer = CommandNormalizer(LanguageResolver(), time)
        matcher = DeterministicIntentMatcher()
    }

    @Test
    fun `matches English and Bengali torch commands`() {
        val onEn = matcher.matchIntent(normalizer.normalize("Hello Meemo, turn on the flashlight"))
        assertEquals(MeemoIntent.TORCH_ON, onEn.intent)
        assertTrue(onEn.confidence >= 0.95f)

        val onBn = matcher.matchIntent(normalizer.normalize("ফ্ল্যাশলাইট চালু করো"))
        assertEquals(MeemoIntent.TORCH_ON, onBn.intent)

        val offBn = matcher.matchIntent(normalizer.normalize("টর্চ বন্ধ করো"))
        assertEquals(MeemoIntent.TORCH_OFF, offBn.intent)
    }

    @Test
    fun `distinguishes OPEN_BROWSER from generic OPEN_APPLICATION`() {
        val browserEn = matcher.matchIntent(normalizer.normalize("Open my browser"))
        assertEquals(MeemoIntent.OPEN_BROWSER, browserEn.intent)

        val browserBn = matcher.matchIntent(normalizer.normalize("আমার ব্রাউজার খোলো"))
        assertEquals(MeemoIntent.OPEN_BROWSER, browserBn.intent)

        val appEn = matcher.matchIntent(normalizer.normalize("Open YouTube"))
        assertEquals(MeemoIntent.OPEN_APPLICATION, appEn.intent)

        val appBn = matcher.matchIntent(normalizer.normalize("ইউটিউব খোলো"))
        assertEquals(MeemoIntent.OPEN_APPLICATION, appBn.intent)
    }

    @Test
    fun `matches battery, device info, volume, media, settings, notifications, and alias intents`() {
        assertEquals(
            MeemoIntent.GET_BATTERY_STATUS,
            matcher.matchIntent(normalizer.normalize("What is my battery status?")).intent
        )
        assertEquals(
            MeemoIntent.GET_BATTERY_STATUS,
            matcher.matchIntent(normalizer.normalize("ব্যাটারি কত?")).intent
        )
        assertEquals(
            MeemoIntent.GET_DEVICE_INFORMATION,
            matcher.matchIntent(normalizer.normalize("Show device information")).intent
        )
        assertEquals(
            MeemoIntent.SET_VOLUME,
            matcher.matchIntent(normalizer.normalize("Set volume to 50 percent")).intent
        )
        assertEquals(
            MeemoIntent.ADJUST_VOLUME,
            matcher.matchIntent(normalizer.normalize("ভলিউম বাড়াও")).intent
        )
        assertEquals(
            MeemoIntent.MEDIA_PLAY,
            matcher.matchIntent(normalizer.normalize("Play music")).intent
        )
        assertEquals(
            MeemoIntent.MEDIA_PAUSE,
            matcher.matchIntent(normalizer.normalize("গান থামাও")).intent
        )
        assertEquals(
            MeemoIntent.OPEN_SETTINGS,
            matcher.matchIntent(normalizer.normalize("Open Wi-Fi settings")).intent
        )
        assertEquals(
            MeemoIntent.READ_NOTIFICATIONS,
            matcher.matchIntent(normalizer.normalize("Read my notifications")).intent
        )
        assertEquals(
            MeemoIntent.STOP_SPEAKING,
            matcher.matchIntent(normalizer.normalize("Stop speaking")).intent
        )
        assertEquals(
            MeemoIntent.CREATE_ALIAS,
            matcher.matchIntent(normalizer.normalize("Whenever I say work browser open Firefox")).intent
        )
    }

    @Test
    fun `returns UNKNOWN for unrelated conversational chatter`() {
        val unknown = matcher.matchIntent(normalizer.normalize("Write me a 500 word poem about clouds"))
        assertEquals(MeemoIntent.UNKNOWN, unknown.intent)
    }
}
