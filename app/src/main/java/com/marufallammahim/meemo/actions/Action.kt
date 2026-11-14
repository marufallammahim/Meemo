package com.marufallammahim.meemo.actions

import com.marufallammahim.meemo.assistant.entity.SettingsTarget
import com.marufallammahim.meemo.assistant.entity.VolumeDirection

/**
 * Typed Action hierarchy for Meemo.
 * Raw strings, arbitrary shell commands, or unvalidated AI payloads can never be executed directly.
 */
sealed interface MeemoAction {
    val requiresConfirmation: Boolean get() = false

    data class OpenApplication(
        val packageName: String,
        val appLabel: String
    ) : MeemoAction

    data class OpenBrowser(
        val preferredPackage: String? = null
    ) : MeemoAction

    data object TurnTorchOn : MeemoAction
    data object TurnTorchOff : MeemoAction
    data object GetBatteryStatus : MeemoAction
    data object GetDeviceInformation : MeemoAction

    data class SetVolume(
        val levelPercent: Int,
        val streamName: String = "MUSIC"
    ) : MeemoAction

    data class AdjustVolume(
        val direction: VolumeDirection
    ) : MeemoAction

    data class MediaControl(
        val command: MediaCommandType
    ) : MeemoAction

    data class NavigateToSettings(
        val target: SettingsTarget,
        val isFallbackForDirectToggle: Boolean = false
    ) : MeemoAction

    data object ReadRecentNotifications : MeemoAction
    data object StopSpeaking : MeemoAction
    data object CancelActiveCommand : MeemoAction

    data class SaveCustomAlias(
        val triggerPhrase: String,
        val targetAppLabel: String,
        val resolvedPackageName: String
    ) : MeemoAction

    data class ExecuteAutomationRoutine(
        val routineName: String,
        val steps: List<MeemoAction>,
        override val requiresConfirmation: Boolean = false
    ) : MeemoAction

    data class ExecuteApprovedRootDiagnostic(
        val policyCommandId: String,
        val parameter: String? = null,
        override val requiresConfirmation: Boolean = true
    ) : MeemoAction
}

enum class MediaCommandType {
    PLAY, PAUSE, NEXT, PREVIOUS
}
