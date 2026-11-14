package com.marufallammahim.meemo.assistant.intent

import com.marufallammahim.meemo.assistant.command.NormalizedCommand
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first deterministic intent matcher supporting English, Bengali (বাংলা),
 * and mixed Banglish commands without broad unsafe substring guessing.
 */
@Singleton
class DeterministicIntentMatcher @Inject constructor() {

    fun matchIntent(command: NormalizedCommand): IntentMatchResult {
        val text = command.normalizedText.trim()
        if (text.isEmpty()) {
            return IntentMatchResult(MeemoIntent.UNKNOWN, 0.0f, "empty_input")
        }
        val lower = text.lowercase()

        // 1. Immediate Interruption / Cancellation commands (Highest Priority)
        if (STOP_SPEAKING_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.STOP_SPEAKING, 0.99f, "stop_speaking_exact")
        }
        if (CANCEL_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.CANCEL_COMMAND, 0.99f, "cancel_command_exact")
        }

        // 2. Alias Creation ("Whenever I say X, open Y")
        if (CREATE_ALIAS_PATTERNS.any { it.containsMatchIn(lower) }) {
            return IntentMatchResult(MeemoIntent.CREATE_ALIAS, 0.95f, "create_alias_pattern")
        }

        // 3. Flashlight / Torch ON & OFF
        if (TORCH_ON_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.TORCH_ON, 0.97f, "torch_on_pattern")
        }
        if (TORCH_OFF_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.TORCH_OFF, 0.97f, "torch_off_pattern")
        }

        // 4. Battery Status & Device Info
        if (BATTERY_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.GET_BATTERY_STATUS, 0.96f, "battery_status_pattern")
        }
        if (DEVICE_INFO_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.GET_DEVICE_INFORMATION, 0.95f, "device_info_pattern")
        }

        // 5. Volume Set & Adjust
        if (SET_VOLUME_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.SET_VOLUME, 0.95f, "set_volume_pattern")
        }
        if (ADJUST_VOLUME_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.ADJUST_VOLUME, 0.94f, "adjust_volume_pattern")
        }

        // 6. Media Transport Controls
        if (MEDIA_PLAY_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.MEDIA_PLAY, 0.94f, "media_play_pattern")
        }
        if (MEDIA_PAUSE_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.MEDIA_PAUSE, 0.94f, "media_pause_pattern")
        }
        if (MEDIA_NEXT_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.MEDIA_NEXT, 0.94f, "media_next_pattern")
        }
        if (MEDIA_PREV_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.MEDIA_PREVIOUS, 0.94f, "media_prev_pattern")
        }

        // 7. Read Notifications Aloud
        if (READ_NOTIFICATIONS_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.READ_NOTIFICATIONS, 0.95f, "read_notifications_pattern")
        }

        // 8. Root Diagnostic Command (Isolated)
        if (ROOT_DIAGNOSTIC_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.ROOT_SYSTEM_DIAGNOSTIC, 0.92f, "root_diagnostic_pattern")
        }

        // 9. Run Automation Routine
        if (RUN_AUTOMATION_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.RUN_AUTOMATION, 0.92f, "run_automation_pattern")
        }

        // 10. Open Browser (Explicit check before generic Open App)
        if (OPEN_BROWSER_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.OPEN_BROWSER, 0.96f, "open_browser_pattern")
        }

        // 11. Open Settings / Protected Toggles (Wi-Fi, Bluetooth, Display, Sound, Accessibility, Notification Settings)
        if (OPEN_SETTINGS_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.OPEN_SETTINGS, 0.94f, "open_settings_pattern")
        }

        // 12. Open Installed Application ("open <app>", "launch <app>", "<app> খোলো", "<app> চালু করো")
        if (OPEN_APP_PATTERNS.any { it.matches(lower) }) {
            return IntentMatchResult(MeemoIntent.OPEN_APPLICATION, 0.91f, "open_application_pattern")
        }

        return IntentMatchResult(MeemoIntent.UNKNOWN, 0.15f, "no_deterministic_match")
    }

    companion object {
        private val STOP_SPEAKING_PATTERNS = listOf(
            Regex("^(stop speaking|stop talking|be quiet|shut up|mute speech|quiet)$"),
            Regex("^(বলা বন্ধ করো|কথা বন্ধ করো|চুপ করো|থামো|বলা থামাও)$"),
            Regex("^(bola bondho koro|kotha bondho koro|thamo)$")
        )

        private val CANCEL_PATTERNS = listOf(
            Regex("^(cancel|cancel command|never mind|abort|stop)$"),
            Regex("^(বাতিল|বাতিল করো|কমান্ড বাতিল করো|বাদ দাও)$"),
            Regex("^(cancel koro|bad dao)$")
        )

        private val CREATE_ALIAS_PATTERNS = listOf(
            Regex("^(whenever i say|when i say|save alias|create alias)\\s+.+\\s+(open|run|launch)\\s+.+$"),
            Regex("^যখন আমি বলব\\s+.+\\s+তখন\\s+.+\\s+(খোলো|চালু করো)$")
        )

        private val TORCH_ON_PATTERNS = listOf(
            Regex("^(please\\s+)?(turn on|switch on|enable|open|start)\\s+(the\\s+)?(flashlight|torch|flash|light)$"),
            Regex("^(flashlight|torch)\\s+(on|enable|chalu koro|on koro)$"),
            Regex("^(ফ্ল্যাশলাইট|টর্চ|টর্চলাইট|আলো)\\s*(চালু করো|অন করো|জ্বালাও|খোলো)$")
        )

        private val TORCH_OFF_PATTERNS = listOf(
            Regex("^(please\\s+)?(turn off|switch off|disable|close|stop)\\s+(the\\s+)?(flashlight|torch|flash|light)$"),
            Regex("^(flashlight|torch)\\s+(off|disable|bondho koro|off koro)$"),
            Regex("^(ফ্ল্যাশলাইট|টর্চ|টর্চলাইট|আলো)\\s*(বন্ধ করো|অফ করো|নেভাও)$")
        )

        private val BATTERY_PATTERNS = listOf(
            Regex("^(what is my |what's my |check |show |get |tell me )?(battery|battery status|battery level|battery percentage|power level)(\\s+status)?$"),
            Regex("^(how much battery is left|battery koto|charge koto)$"),
            Regex("^(ব্যাটারি কত|ব্যাটারি স্ট্যাটাস|চার্জ কত|ব্যাটারির অবস্থা কী|ব্যাটারি লেভেল কত)$")
        )

        private val DEVICE_INFO_PATTERNS = listOf(
            Regex("^(what is my |show |check |get )?(device info|device information|phone info|system info|android version|hardware status)$"),
            Regex("^(ডিভাইস তথ্য|ফোনের তথ্য|সিস্টেম তথ্য|অ্যান্ড্রয়েড ভার্সন কত)$")
        )

        private val SET_VOLUME_PATTERNS = listOf(
            Regex("^(set|change)\\s+(the\\s+)?(media\\s+|music\\s+|ring\\s+|alarm\\s+)?volume\\s+to\\s+\\d{1,3}(\\s*%?|\\s*percent)?$"),
            Regex("^volume\\s+\\d{1,3}(\\s*%?|\\s*percent)?$"),
            Regex("^(ভলিউম|সাউন্ড)\\s+\\d{1,3}(\\s*%|\\s*শতাংশে|\\s*পার্সেন্টে)?\\s*(সেট করো|করো|রাখো)$")
        )

        private val ADJUST_VOLUME_PATTERNS = listOf(
            Regex("^(turn |volume )?(up|down|increase|decrease|raise|lower|mute|unmute)(\\s+the)?(\\s+volume)?$"),
            Regex("^(increase|decrease|raise|lower|mute|unmute)\\s+(the\\s+)?volume$"),
            Regex("^(ভলিউম|সাউন্ড)\\s*(বাড়াও|কমাও|মিউট করো|আনমিউট করো)$"),
            Regex("^(volume|sound)\\s+(barao|komao|mute koro)$")
        )

        private val MEDIA_PLAY_PATTERNS = listOf(
            Regex("^(play|resume)(\\s+music|\\s+media|\\s+song|\\s+audio)?$"),
            Regex("^(গান চালাও|মিডিয়া চালাও|প্লে করো)$")
        )

        private val MEDIA_PAUSE_PATTERNS = listOf(
            Regex("^(pause)(\\s+music|\\s+media|\\s+song|\\s+audio)?$"),
            Regex("^(গান থামাও|পজ করো|মিডিয়া থামাও)$")
        )

        private val MEDIA_NEXT_PATTERNS = listOf(
            Regex("^(next|skip)(\\s+track|\\s+song|\\s+media)?$"),
            Regex("^(পরের গান|নেক্সট গান|পরবর্তী গান)$")
        )

        private val MEDIA_PREV_PATTERNS = listOf(
            Regex("^(previous|prev)(\\s+track|\\s+song|\\s+media)$"),
            Regex("^(আগের গান|পূর্ববর্তী গান)$")
        )

        private val READ_NOTIFICATIONS_PATTERNS = listOf(
            Regex("^(read|check|speak|tell me)(\\s+my)?\\s+(notifications|messages|new messages|unread messages)(\\s+aloud)?$"),
            Regex("^(নোটিফিকেশন পড়ো|মেসেজ পড়ো|নতুন মেসেজ শোনাও|নোটিফিকেশন শোনাও)$")
        )

        private val ROOT_DIAGNOSTIC_PATTERNS = listOf(
            Regex("^(run\\s+)?root\\s+(diagnostic|battery dump|thermal status|package check|check)(\\s+.+)?$"),
            Regex("^রুট\\s+(ডায়াগনস্টিক|পরীক্ষা|স্ট্যাটাস)$")
        )

        private val RUN_AUTOMATION_PATTERNS = listOf(
            Regex("^(run|execute|start)\\s+(routine|automation)\\s+.+$"),
            Regex("^রুটিন\\s+চালু\\s+করো\\s+.+$")
        )

        private val OPEN_BROWSER_PATTERNS = listOf(
            Regex("^(please\\s+)?(open|launch|start)\\s+(my\\s+|the\\s+|default\\s+)?(web\\s+)?browser$"),
            Regex("^(আমার\\s+)?(ব্রাউজার|ওয়েব ব্রাউজার)\\s*(খোলো|চালু করো|ওপেন করো)$"),
            Regex("^(browser|my browser)\\s+(kholo|open koro|chalu koro)$")
        )

        private val OPEN_SETTINGS_PATTERNS = listOf(
            Regex("^(open|go to|show|launch|turn on|turn off|enable|disable)\\s+(the\\s+)?(wifi|wi-fi|bluetooth|display|sound|accessibility|notification|app|system)?(\\s*settings)?$").let {
                // Only match if settings or a protected connectivity toggle (wifi/bluetooth/accessibility) is explicitly named
                Regex("^(open|go to|show|launch|turn on|turn off|enable|disable)\\s+(the\\s+)?((wifi|wi-fi|bluetooth|accessibility|notification access)(\\s+settings)?|(display|sound|system|device|app)?\\s*settings)$")
            },
            Regex("^(ওয়াইফাই|ব্লুটুথ|সেটিংস|ডিসপ্লে সেটিংস|সাউন্ড সেটিংস|অ্যাক্সেসিবিলিটি সেটিংস)\\s*(খোলো|চালু করো|বন্ধ করো|দেখাও)$")
        )

        private val OPEN_APP_PATTERNS = listOf(
            Regex("^(please\\s+)?(open|launch|start)\\s+(?!my\\s+browser$|the\\s+browser$|browser$)(.+)$"),
            Regex("^(.+?)\\s+(অ্যাপ\\s+)?(খোলো|চালু করো|ওপেন করো)$"),
            Regex("^(.+?)\\s+(kholo|open koro|chalu koro)$")
        )
    }
}
