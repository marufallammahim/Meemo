package com.marufallammahim.meemo.core.permissions

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.marufallammahim.meemo.accessibility.MeemoAccessibilityService
import com.marufallammahim.meemo.notifications.MeemoNotificationListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PermissionChecker {
    fun hasRecordAudioPermission(): Boolean
    fun hasPostNotificationsPermission(): Boolean
    fun isNotificationListenerEnabled(): Boolean
    fun isAccessibilityServiceEnabled(): Boolean
}

@Singleton
class AndroidPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionChecker {

    override fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasPostNotificationsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val expectedComponent = ComponentName(context, MeemoNotificationListener::class.java).flattenToString()
        return flat.split(":").any { it.equals(expectedComponent, ignoreCase = true) }
    }

    override fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expectedComponent = ComponentName(context, MeemoAccessibilityService::class.java).flattenToString()
        return enabledServices.split(":").any { it.equals(expectedComponent, ignoreCase = true) }
    }
}
