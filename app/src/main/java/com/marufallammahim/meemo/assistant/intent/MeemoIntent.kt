package com.marufallammahim.meemo.assistant.intent

enum class MeemoIntent(
    val requiresHardwareCapability: String? = null,
    val isSensitive: Boolean = false
) {
    OPEN_APPLICATION(requiresHardwareCapability = "CAP_APP_LAUNCH"),
    OPEN_BROWSER(requiresHardwareCapability = "CAP_BROWSER_RESOLVE"),
    TORCH_ON(requiresHardwareCapability = "CAP_TORCH"),
    TORCH_OFF(requiresHardwareCapability = "CAP_TORCH"),
    GET_BATTERY_STATUS(requiresHardwareCapability = "CAP_BATTERY_INFO"),
    GET_DEVICE_INFORMATION(requiresHardwareCapability = "CAP_DEVICE_INFO"),
    SET_VOLUME(requiresHardwareCapability = "CAP_AUDIO_VOLUME"),
    ADJUST_VOLUME(requiresHardwareCapability = "CAP_AUDIO_VOLUME"),
    MEDIA_PLAY(requiresHardwareCapability = "CAP_MEDIA_CONTROL"),
    MEDIA_PAUSE(requiresHardwareCapability = "CAP_MEDIA_CONTROL"),
    MEDIA_NEXT(requiresHardwareCapability = "CAP_MEDIA_CONTROL"),
    MEDIA_PREVIOUS(requiresHardwareCapability = "CAP_MEDIA_CONTROL"),
    OPEN_SETTINGS(requiresHardwareCapability = "CAP_SETTINGS_NAVIGATION"),
    READ_NOTIFICATIONS(requiresHardwareCapability = "CAP_NOTIFICATION_READER"),
    STOP_SPEAKING(requiresHardwareCapability = null),
    CANCEL_COMMAND(requiresHardwareCapability = null),
    CREATE_ALIAS(requiresHardwareCapability = null),
    RUN_AUTOMATION(requiresHardwareCapability = null),
    ROOT_SYSTEM_DIAGNOSTIC(requiresHardwareCapability = "CAP_ROOT_EXECUTION", isSensitive = true),
    UNKNOWN(requiresHardwareCapability = null);

    companion object {
        fun fromNameOrUnknown(name: String): MeemoIntent =
            entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

data class IntentMatchResult(
    val intent: MeemoIntent,
    val confidence: Float,
    val matchedPatternDescription: String
)
