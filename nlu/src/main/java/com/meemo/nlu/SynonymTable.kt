package com.meemo.nlu

class SynonymTable {
    private val openSuffixPatterns = listOf(
        Regex("^(.+?)\\s+(?:kholo|khulun|open koro|chalu koro)$") to "open $1",
        Regex("^(.+?)\\s+(?:খোলো|খুলুন|ওপেন করো|চালু করো)$") to "open $1"
    )

    private val phraseReplacements = listOf(
        Regex("\\bswitch\\s+on\\b") to "on",
        Regex("\\bturn\\s+on\\b") to "on",
        Regex("\\bturn\\s+off\\b") to "off",
        Regex("\\bswitch\\s+off\\b") to "off",
        Regex("\\bwi\\s+fi\\b") to "wifi",
        Regex("ওয়াই\\s+ফাই") to "wifi",
        Regex("গুগল\\s+ক্রোম") to "chrome"
    )

    private val words = mapOf(
        "torch" to "flashlight",
        "flash" to "flashlight",
        "light" to "flashlight",
        "enable" to "on",
        "start" to "open",
        "launch" to "open",
        "run" to "open",
        "wireless" to "wifi",
        "ব্রাউজার" to "browser",
        "ওয়েব" to "web",
        "ইন্টারনেট" to "internet",
        "ওয়াইফাই" to "wifi",
        "ব্লুটুথ" to "bluetooth",
        "ডিসপ্লে" to "display",
        "স্ক্রিন" to "screen",
        "সাউন্ড" to "sound",
        "ব্যাটারি" to "battery",
        "সেটিংস" to "settings",
        "সেটিং" to "settings",
        "নোটিফিকেশন" to "notifications",
        "অ্যাক্সেসিবিলিটি" to "accessibility",
        "স্ট্যাটাস" to "status",
        "লেভেল" to "level",
        "চার্জ" to "battery",
        "কত" to "koto",
        "কতো" to "koto",
        "আছে" to "ache",
        "ক্যালকুলেটর" to "calculator",
        "ক্যামেরা" to "camera",
        "ক্রোম" to "chrome",
        "ফায়ারফক্স" to "firefox",
        "ফাইল" to "files",
        "ফাইলস" to "files",
        "গ্যালারি" to "gallery",
        "ইউটিউব" to "youtube",
        "জিমেইল" to "gmail",
        "হোয়াটসঅ্যাপ" to "whatsapp",
        "ফেসবুক" to "facebook",
        "ইনস্টাগ্রাম" to "instagram",
        "টেলিগ্রাম" to "telegram"
    )

    private val wholePhraseReplacements = listOf(
        Regex("^battery\\s+koto(?:\\s+ache)?$") to "battery"
    )

    fun canonicalize(input: String): String {
        var result = input
        openSuffixPatterns.forEach { (pattern, replacement) ->
            result = pattern.replace(result, replacement)
        }
        phraseReplacements.forEach { (pattern, replacement) ->
            result = pattern.replace(result, replacement)
        }
        result = result.split(Regex("\\s+")).joinToString(" ") { words[it] ?: it }
        wholePhraseReplacements.forEach { (pattern, replacement) ->
            result = pattern.replace(result, replacement)
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }
}
