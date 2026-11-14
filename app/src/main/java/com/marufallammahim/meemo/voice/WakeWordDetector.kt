package com.marufallammahim.meemo.voice

import com.marufallammahim.meemo.core.permissions.PermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class WakeWordDetectorStatus {
    DISABLED_BY_USER,
    MISSING_MIC_PERMISSION,
    ACTIVE_FOREGROUND_SESSION,
    PAUSED_BY_SYSTEM
}

/**
 * Wake-word detector abstraction for "Hello Meemo" / "হ্যালো মিমো".
 * Honestly reports foreground-service status and respects Android background microphone rules.
 */
@Singleton
class WakeWordDetector @Inject constructor(
    private val permissionChecker: PermissionChecker
) {
    private val _status = MutableStateFlow(WakeWordDetectorStatus.DISABLED_BY_USER)
    val status: StateFlow<WakeWordDetectorStatus> = _status.asStateFlow()

    fun evaluateStatus(userOptedIn: Boolean, isForegroundServiceRunning: Boolean): WakeWordDetectorStatus {
        val resolved = when {
            !userOptedIn -> WakeWordDetectorStatus.DISABLED_BY_USER
            !permissionChecker.hasRecordAudioPermission() -> WakeWordDetectorStatus.MISSING_MIC_PERMISSION
            isForegroundServiceRunning -> WakeWordDetectorStatus.ACTIVE_FOREGROUND_SESSION
            else -> WakeWordDetectorStatus.PAUSED_BY_SYSTEM
        }
        _status.value = resolved
        return resolved
    }

    fun containsWakePhrase(transcript: String): Boolean {
        val lower = transcript.trim().lowercase()
        return lower.startsWith("hello meemo") ||
            lower.startsWith("hey meemo") ||
            lower.startsWith("হ্যালো মিমো") ||
            lower.startsWith("মিমো")
    }
}
