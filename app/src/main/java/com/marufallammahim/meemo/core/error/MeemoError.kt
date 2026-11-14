package com.marufallammahim.meemo.core.error

/**
 * Structured error taxonomy for Meemo.
 * Distinguishes expected operational limitations from unexpected errors,
 * providing stable error codes, user-friendly bilingual explanations, and recovery actions.
 */
enum class RecoveryActionType {
    NONE,
    RETRY,
    REPHRASE_COMMAND,
    SELECT_CANDIDATE_APP,
    REQUEST_RUNTIME_PERMISSION,
    OPEN_APP_SETTINGS,
    OPEN_NOTIFICATION_LISTENER_SETTINGS,
    OPEN_ACCESSIBILITY_SETTINGS,
    OPEN_SYSTEM_SETTINGS,
    CONFIGURE_AI_CREDENTIALS
}

sealed class MeemoError(
    val code: String,
    val titleEn: String,
    val titleBn: String,
    val messageEn: String,
    val messageBn: String,
    val recoveryAction: RecoveryActionType,
    val isRetryable: Boolean = false,
    val diagnosticId: String = "DIAG-${code.hashCode().ushr(1)}"
) {
    data object EmptyCommand : MeemoError(
        code = "ERR_EMPTY_COMMAND",
        titleEn = "Empty Command",
        titleBn = "খালি কমান্ড",
        messageEn = "Please speak or type a command for Meemo.",
        messageBn = "অনুগ্রহ করে মিমোর জন্য একটি কমান্ড বলুন বা লিখুন।",
        recoveryAction = RecoveryActionType.REPHRASE_COMMAND
    )

    data class UnrecognizedCommand(val rawInput: String) : MeemoError(
        code = "ERR_COMMAND_UNRECOGNIZED",
        titleEn = "Command Not Recognized",
        titleBn = "কমান্ডটি বোঝা যায়নি",
        messageEn = "Meemo could not match that phrase to a supported action. Try 'Open browser', 'Turn on flashlight', or 'Battery status'.",
        messageBn = "মিমো এই কথাটির সাথে কোনো সমর্থিত কাজ মেলাতে পারেনি। 'ব্রাউজার খোলো', 'টর্চ চালু করো', বা 'ব্যাটারি কত' চেষ্টা করুন।",
        recoveryAction = RecoveryActionType.REPHRASE_COMMAND
    )

    data class AmbiguousApplication(val query: String, val candidates: List<String>) : MeemoError(
        code = "ERR_APP_AMBIGUOUS",
        titleEn = "Multiple Apps Match",
        titleBn = "একাধিক অ্যাপ পাওয়া গেছে",
        messageEn = "Multiple installed applications match '$query': ${candidates.joinToString(", ")}. Please select the exact app.",
        messageBn = "'$query' নামের একাধিক অ্যাপ পাওয়া গেছে: ${candidates.joinToString(", ")}। অনুগ্রহ করে নির্দিষ্ট অ্যাপটি বেছে নিন।",
        recoveryAction = RecoveryActionType.SELECT_CANDIDATE_APP
    )

    data class ApplicationNotFound(val appName: String) : MeemoError(
        code = "ERR_APP_NOT_FOUND",
        titleEn = "App Not Installed",
        titleBn = "অ্যাপটি ইনস্টল করা নেই",
        messageEn = "No launchable application matching '$appName' was found on this device.",
        messageBn = "এই ডিভাইসে '$appName' নামের কোনো চালু করার মতো অ্যাপ পাওয়া যায়নি।",
        recoveryAction = RecoveryActionType.REPHRASE_COMMAND
    )

    data object BrowserNotFound : MeemoError(
        code = "ERR_BROWSER_NOT_FOUND",
        titleEn = "No Browser Found",
        titleBn = "কোনো ব্রাউজার পাওয়া যায়নি",
        messageEn = "No compatible web browser application is visible on this device.",
        messageBn = "এই ডিভাইসে কোনো ওয়েব ব্রাউজার অ্যাপ খুঁজে পাওয়া যায়নি।",
        recoveryAction = RecoveryActionType.OPEN_SYSTEM_SETTINGS
    )

    data class PermissionDenied(
        val permissionName: String,
        val rationaleEn: String,
        val rationaleBn: String,
        val targetRecovery: RecoveryActionType = RecoveryActionType.REQUEST_RUNTIME_PERMISSION
    ) : MeemoError(
        code = "ERR_PERMISSION_DENIED",
        titleEn = "Permission Required",
        titleBn = "অনুমতি প্রয়োজন",
        messageEn = rationaleEn,
        messageBn = rationaleBn,
        recoveryAction = targetRecovery,
        isRetryable = true
    )

    data class HardwareUnavailable(val featureName: String) : MeemoError(
        code = "ERR_HARDWARE_UNAVAILABLE",
        titleEn = "Hardware Unavailable",
        titleBn = "হার্ডওয়্যার উপলব্ধ নয়",
        messageEn = "This device does not have the required $featureName hardware or it is currently in use.",
        messageBn = "এই ডিভাইসে প্রয়োজনীয় $featureName হার্ডওয়্যারটি নেই অথবা সেটি বর্তমানে অন্য কাজে ব্যবহৃত হচ্ছে।",
        recoveryAction = RecoveryActionType.NONE
    )

    data class PlatformRestricted(val operationName: String, val minimumSdkOrReason: String) : MeemoError(
        code = "ERR_PLATFORM_RESTRICTED",
        titleEn = "Restricted by Android OS",
        titleBn = "অ্যান্ড্রয়েড সিস্টেম দ্বারা সীমাবদ্ধ",
        messageEn = "Android prevents apps from directly performing '$operationName' ($minimumSdkOrReason). You can complete it in System Settings.",
        messageBn = "অ্যান্ড্রয়েড সরাসরি '$operationName' করার অনুমতি দেয় না ($minimumSdkOrReason)। আপনি সিস্টেম সেটিংস থেকে এটি করতে পারেন।",
        recoveryAction = RecoveryActionType.OPEN_SYSTEM_SETTINGS
    )

    data class SpeechUnavailable(val reason: String) : MeemoError(
        code = "ERR_SPEECH_UNAVAILABLE",
        titleEn = "Speech Recognition Unavailable",
        titleBn = "ভয়েস রিকগনিশন উপলব্ধ নয়",
        messageBn = "ভয়েস রিকগনিশন সম্পন্ন করা যায়নি ($reason)। আপনি টেক্সট লিখে কমান্ড দিতে পারেন।",
        messageEn = "Speech recognition could not complete ($reason). You can continue using typed commands.",
        recoveryAction = RecoveryActionType.RETRY,
        isRetryable = true
    )

    data class TtsUnavailable(val localeName: String) : MeemoError(
        code = "ERR_TTS_VOICE_MISSING",
        titleEn = "Voice Output Unavailable",
        titleBn = "ভয়েস আউটপুট উপলব্ধ নয়",
        messageEn = "Text-to-speech voice for $localeName is not installed on this device. Showing text response instead.",
        messageBn = "এই ডিভাইসে $localeName ভাষার ভয়েস প্যাক ইনস্টল করা নেই। টেক্সট উত্তর দেখানো হচ্ছে।",
        recoveryAction = RecoveryActionType.OPEN_SYSTEM_SETTINGS
    )

    data class RootUnavailableOrRejected(val reason: String) : MeemoError(
        code = "ERR_ROOT_REJECTED",
        titleEn = "Root Operation Unavailable",
        titleBn = "রুট অপারেশন উপলব্ধ নয়",
        messageEn = "Privileged root execution was blocked or unavailable: $reason. Continuing safely in non-root mode.",
        messageBn = "রুট কমান্ড কার্যকর করা যায়নি: $reason। সাধারণ মোডে নিরাপদে কাজ চলছে।",
        recoveryAction = RecoveryActionType.OPEN_SYSTEM_SETTINGS
    )

    data class AiProviderFailure(val reason: String) : MeemoError(
        code = "ERR_AI_PROVIDER",
        titleEn = "AI Interpretation Unavailable",
        titleBn = "এআই সুবিধা উপলব্ধ নয়",
        messageEn = "Optional AI interpretation failed ($reason). Using local offline command engine.",
        messageBn = "ঐচ্ছিক এআই সংযোগ বিচ্ছিন্ন ($reason)। লোকাল অফলাইন ইঞ্জিন ব্যবহার করা হচ্ছে।",
        recoveryAction = RecoveryActionType.CONFIGURE_AI_CREDENTIALS,
        isRetryable = true
    )

    data class InternalFailure(val safeContext: String) : MeemoError(
        code = "ERR_INTERNAL",
        titleEn = "Unexpected Error",
        titleBn = "অপ্রত্যাশিত ত্রুটি",
        messageEn = "An unexpected error occurred during $safeContext. No unsafe actions were taken.",
        messageBn = "$safeContext চলাকালীন একটি অপ্রত্যাশিত ত্রুটি ঘটেছে।",
        recoveryAction = RecoveryActionType.RETRY,
        isRetryable = true
    )
}
