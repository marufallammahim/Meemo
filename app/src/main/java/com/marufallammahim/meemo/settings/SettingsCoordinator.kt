package com.marufallammahim.meemo.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsCoordinator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun openNotificationListenerSettings() {
        launchSafe(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openAccessibilitySettings() {
        launchSafe(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openAppDetailsSettings() {
        launchSafe(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    }

    fun openTtsSettings() {
        launchSafe(Intent("com.android.settings.TTS_SETTINGS"))
    }

    private fun launchSafe(intent: Intent) {
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
