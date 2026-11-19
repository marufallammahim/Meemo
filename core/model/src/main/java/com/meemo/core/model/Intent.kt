package com.meemo.core.model

sealed interface MeemoIntent {
    data object TorchOn : MeemoIntent
    data object TorchOff : MeemoIntent
    data class SetVolume(val percent: Int?, val delta: Int?) : MeemoIntent
    data object GetBattery : MeemoIntent
    data class OpenApp(val query: String) : MeemoIntent
    data object OpenBrowser : MeemoIntent
    data class OpenSettings(val page: SettingsPage) : MeemoIntent
    data class Media(val action: MediaAction) : MeemoIntent
    data class SetRinger(val mode: RingerMode) : MeemoIntent
    data class DefineAlias(val phrase: String, val target: String) : MeemoIntent
    data class Unknown(val raw: String) : MeemoIntent
}

enum class SettingsPage {
    MAIN, WIFI, BLUETOOTH, DISPLAY, SOUND, BATTERY, ACCESSIBILITY, NOTIFICATIONS
}

enum class MediaAction { PLAY, PAUSE, NEXT, PREVIOUS }
enum class RingerMode { NORMAL, VIBRATE, SILENT }
enum class InputSource { VOICE, TEXT }
