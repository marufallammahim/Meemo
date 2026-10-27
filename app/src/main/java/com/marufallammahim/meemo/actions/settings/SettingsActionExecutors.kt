package com.marufallammahim.meemo.actions.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.assistant.entity.SettingsTarget
import com.marufallammahim.meemo.core.error.MeemoError
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun executeNavigateSettings(action: MeemoAction.NavigateToSettings): ActionResult {
        val intent = when (action.target) {
            SettingsTarget.WIFI_SETTINGS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_WIFI)
                } else {
                    Intent(Settings.ACTION_WIFI_SETTINGS)
                }
            }
            SettingsTarget.BLUETOOTH_SETTINGS -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            SettingsTarget.DISPLAY_SETTINGS -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            SettingsTarget.SOUND_SETTINGS -> Intent(Settings.ACTION_SOUND_SETTINGS)
            SettingsTarget.NOTIFICATION_LISTENER_SETTINGS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            SettingsTarget.ACCESSIBILITY_SETTINGS -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            SettingsTarget.APPLICATION_DETAILS_SETTINGS -> Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
            SettingsTarget.MAIN_SETTINGS -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            if (action.isFallbackForDirectToggle) {
                ActionResult.PartialSuccess(
                    summaryEn = "Opened ${action.target.name.replace('_', ' ').lowercase()} panel.",
                    summaryBn = "সিস্টেম সেটিংস প্যানেল খোলা হয়েছে।",
                    reasonEn = "Android 10+ requires user interaction to toggle Wi-Fi or Bluetooth directly",
                    reasonBn = "অ্যান্ড্রয়েড ১০+ এ সরাসরি পরিবর্তনের জন্য ব্যবহারকারীর নিশ্চিতকরণ প্রয়োজন"
                )
            } else {
                ActionResult.Success(
                    summaryEn = "Opened ${action.target.name.replace('_', ' ').lowercase()}.",
                    summaryBn = "সেটিংস স্ক্রিন খোলা হয়েছে।",
                    verifiedBySystem = true
                )
            }
        } catch (e: Exception) {
            ActionResult.Failed(MeemoError.PlatformRestricted(action.target.name, "Settings screen unavailable"))
        }
    }
}
