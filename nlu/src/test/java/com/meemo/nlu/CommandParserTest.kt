package com.meemo.nlu

import com.meemo.core.model.MeemoIntent
import com.meemo.core.model.SettingsPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CommandParserTest(private val input: String, private val expected: MeemoIntent) {
    @Test
    fun parsesSupportedTextCommands() {
        val command = CommandParser().parse(input)
        assertEquals(expected, command.intent)
        assertTrue(command.confidence >= 0.8f)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun commands(): Collection<Array<Any>> = listOf(
            arrayOf("battery", MeemoIntent.GetBattery),
            arrayOf("battery status", MeemoIntent.GetBattery),
            arrayOf("check battery", MeemoIntent.GetBattery),
            arrayOf("show battery", MeemoIntent.GetBattery),
            arrayOf("get battery", MeemoIntent.GetBattery),
            arrayOf("hello meemo battery", MeemoIntent.GetBattery),
            arrayOf("hey meemo check battery", MeemoIntent.GetBattery),
            arrayOf("please battery", MeemoIntent.GetBattery),
            arrayOf("can you battery", MeemoIntent.GetBattery),
            arrayOf("could you battery", MeemoIntent.GetBattery),
            arrayOf("open browser", MeemoIntent.OpenBrowser),
            arrayOf("browser", MeemoIntent.OpenBrowser),
            arrayOf("open web browser", MeemoIntent.OpenBrowser),
            arrayOf("browse internet", MeemoIntent.OpenBrowser),
            arrayOf("launch browser", MeemoIntent.OpenBrowser),
            arrayOf("start browser", MeemoIntent.OpenBrowser),
            arrayOf("run browser", MeemoIntent.OpenBrowser),
            arrayOf("open wifi settings", MeemoIntent.OpenSettings(SettingsPage.WIFI)),
            arrayOf("open wi fi settings", MeemoIntent.OpenSettings(SettingsPage.WIFI)),
            arrayOf("open wireless settings", MeemoIntent.OpenSettings(SettingsPage.WIFI)),
            arrayOf("wifi settings", MeemoIntent.OpenSettings(SettingsPage.WIFI)),
            arrayOf("network settings", MeemoIntent.OpenSettings(SettingsPage.WIFI)),
            arrayOf("go to wifi", MeemoIntent.OpenSettings(SettingsPage.WIFI)),
            arrayOf("open bluetooth settings", MeemoIntent.OpenSettings(SettingsPage.BLUETOOTH)),
            arrayOf("bluetooth settings", MeemoIntent.OpenSettings(SettingsPage.BLUETOOTH)),
            arrayOf("open display settings", MeemoIntent.OpenSettings(SettingsPage.DISPLAY)),
            arrayOf("display settings", MeemoIntent.OpenSettings(SettingsPage.DISPLAY)),
            arrayOf("open screen settings", MeemoIntent.OpenSettings(SettingsPage.DISPLAY)),
            arrayOf("open sound settings", MeemoIntent.OpenSettings(SettingsPage.SOUND)),
            arrayOf("sound settings", MeemoIntent.OpenSettings(SettingsPage.SOUND)),
            arrayOf("open battery settings", MeemoIntent.OpenSettings(SettingsPage.BATTERY)),
            arrayOf("battery settings", MeemoIntent.OpenSettings(SettingsPage.BATTERY)),
            arrayOf("open accessibility settings", MeemoIntent.OpenSettings(SettingsPage.ACCESSIBILITY)),
            arrayOf("accessibility settings", MeemoIntent.OpenSettings(SettingsPage.ACCESSIBILITY)),
            arrayOf("open notification settings", MeemoIntent.OpenSettings(SettingsPage.NOTIFICATIONS)),
            arrayOf("notification settings", MeemoIntent.OpenSettings(SettingsPage.NOTIFICATIONS)),
            arrayOf("open settings", MeemoIntent.OpenSettings(SettingsPage.MAIN)),
            arrayOf("settings", MeemoIntent.OpenSettings(SettingsPage.MAIN)),
            arrayOf("open chrome", MeemoIntent.OpenApp("chrome")),
            arrayOf("launch firefox", MeemoIntent.OpenApp("firefox")),
            arrayOf("start calculator", MeemoIntent.OpenApp("calculator")),
            arrayOf("run camera", MeemoIntent.OpenApp("camera")),
            arrayOf("open files", MeemoIntent.OpenApp("files")),
            arrayOf("whenever i say work open chrome", MeemoIntent.DefineAlias("work", "chrome")),
            arrayOf("হ্যালো মিমো ব্যাটারি কত", MeemoIntent.GetBattery),
            arrayOf("ব্যাটারি স্ট্যাটাস", MeemoIntent.GetBattery),
            arrayOf("ব্যাটারি লেভেল", MeemoIntent.GetBattery),
            arrayOf("চার্জ কত আছে", MeemoIntent.GetBattery),
            arrayOf("ব্রাউজার খোলো", MeemoIntent.OpenBrowser),
            arrayOf("ইন্টারনেট ব্রাউজার খোলো", MeemoIntent.OpenBrowser),
            arrayOf("ওয়াইফাই সেটিংস খোলো", MeemoIntent.OpenSettings(SettingsPage.WIFI)),
            arrayOf("ওয়াই-ফাই সেটিংস খোলো", MeemoIntent.OpenSettings(SettingsPage.WIFI)),
            arrayOf("wifi settings kholo", MeemoIntent.OpenSettings(SettingsPage.WIFI)),
            arrayOf("ব্লুটুথ সেটিংস খোলো", MeemoIntent.OpenSettings(SettingsPage.BLUETOOTH)),
            arrayOf("ডিসপ্লে সেটিংস খোলো", MeemoIntent.OpenSettings(SettingsPage.DISPLAY)),
            arrayOf("সাউন্ড সেটিংস খোলো", MeemoIntent.OpenSettings(SettingsPage.SOUND)),
            arrayOf("নোটিফিকেশন সেটিংস খোলো", MeemoIntent.OpenSettings(SettingsPage.NOTIFICATIONS)),
            arrayOf("অ্যাক্সেসিবিলিটি সেটিংস খোলো", MeemoIntent.OpenSettings(SettingsPage.ACCESSIBILITY)),
            arrayOf("সেটিংস খোলো", MeemoIntent.OpenSettings(SettingsPage.MAIN)),
            arrayOf("Chrome kholo", MeemoIntent.OpenApp("chrome")),
            arrayOf("calculator chalu koro", MeemoIntent.OpenApp("calculator")),
            arrayOf("Chrome চালু করো", MeemoIntent.OpenApp("chrome")),
            arrayOf("ক্যালকুলেটর খোলো", MeemoIntent.OpenApp("calculator")),
            arrayOf("ক্যামেরা খোলো", MeemoIntent.OpenApp("camera")),
            arrayOf("ক্রোম খোলো", MeemoIntent.OpenApp("chrome")),
            arrayOf("গুগল ক্রোম চালু করো", MeemoIntent.OpenApp("chrome")),
            arrayOf("ইউটিউব খোলো", MeemoIntent.OpenApp("youtube")),
            arrayOf("set volume to 50 percent", MeemoIntent.SetVolume(50, null)),
            arrayOf("volume 50%", MeemoIntent.SetVolume(50, null)),
            arrayOf("ভলিউম ৫০ সেট করো", MeemoIntent.SetVolume(50, null)),
            arrayOf("ভলিউম বাড়াও", MeemoIntent.SetVolume(null, 1)),
            arrayOf("volume barao", MeemoIntent.SetVolume(null, 1)),
            arrayOf("ভলিউম কমাও", MeemoIntent.SetVolume(null, -1)),
            arrayOf("decrease volume", MeemoIntent.SetVolume(null, -1))
        )
    }
}
