package com.marufallammahim.meemo.actions.media

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.actions.MediaCommandType
import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.assistant.entity.VolumeDirection
import com.marufallammahim.meemo.core.error.MeemoError
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class VolumeMediaActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager? by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    fun executeSetVolume(action: MeemoAction.SetVolume): ActionResult {
        val manager = audioManager
            ?: return ActionResult.Unavailable(MeemoError.HardwareUnavailable("AudioManager"))

        val stream = when (action.streamName.uppercase()) {
            "RING" -> AudioManager.STREAM_RING
            "ALARM" -> AudioManager.STREAM_ALARM
            else -> AudioManager.STREAM_MUSIC
        }
        return try {
            val maxVol = manager.getStreamMaxVolume(stream).coerceAtLeast(1)
            val targetIndex = ((action.levelPercent / 100.0f) * maxVol).roundToInt().coerceIn(0, maxVol)
            manager.setStreamVolume(stream, targetIndex, AudioManager.FLAG_SHOW_UI)
            val verifiedIndex = manager.getStreamVolume(stream)
            val verifiedPct = ((verifiedIndex.toFloat() / maxVol) * 100).roundToInt()

            ActionResult.Success(
                summaryEn = "${action.streamName.lowercase().replaceFirstChar { it.uppercase() }} volume set to $verifiedPct%.",
                summaryBn = "ভলিউম $verifiedPct% এ সেট করা হয়েছে।",
                verifiedBySystem = true,
                details = mapOf("volumePercent" to "$verifiedPct%")
            )
        } catch (e: SecurityException) {
            ActionResult.PartialSuccess(
                summaryEn = "Do Not Disturb restricts volume changes.",
                summaryBn = "ডু নট ডিস্টার্ব চালু থাকায় ভলিউম পরিবর্তন সীমাবদ্ধ।",
                reasonEn = "Adjust volume manually or grant Do Not Disturb access",
                reasonBn = "ম্যানুয়ালি ভলিউম পরিবর্তন করুন"
            )
        }
    }

    fun executeAdjustVolume(action: MeemoAction.AdjustVolume): ActionResult {
        val manager = audioManager
            ?: return ActionResult.Unavailable(MeemoError.HardwareUnavailable("AudioManager"))

        return try {
            when (action.direction) {
                VolumeDirection.UP -> manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                VolumeDirection.DOWN -> manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                VolumeDirection.MUTE -> manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
                VolumeDirection.UNMUTE -> manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            ActionResult.Success(
                summaryEn = "Adjusted media volume (${action.direction.name.lowercase()}).",
                summaryBn = "ভলিউম পরিবর্তন করা হয়েছে (${action.direction.name})।",
                verifiedBySystem = true
            )
        } catch (e: Exception) {
            ActionResult.Failed(MeemoError.InternalFailure("Audio adjustment"))
        }
    }

    fun executeMediaControl(action: MeemoAction.MediaControl): ActionResult {
        val manager = audioManager
            ?: return ActionResult.Unavailable(MeemoError.HardwareUnavailable("AudioManager"))

        val keyCode = when (action.command) {
            MediaCommandType.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
            MediaCommandType.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
            MediaCommandType.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaCommandType.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        }

        return try {
            manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            ActionResult.Success(
                summaryEn = "Sent media ${action.command.name.lowercase()} command to active player.",
                summaryBn = "মিডিয়া প্লেয়ারে ${action.command.name} কমান্ড পাঠানো হয়েছে।",
                verifiedBySystem = false
            )
        } catch (e: Exception) {
            ActionResult.Failed(MeemoError.InternalFailure("Media key dispatch"))
        }
    }
}
