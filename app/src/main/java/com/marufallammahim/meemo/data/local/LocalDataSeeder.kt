package com.marufallammahim.meemo.data.local

import com.marufallammahim.meemo.data.database.entity.PluginRegistryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataSeeder @Inject constructor() {
    fun getBuiltInPlugins(nowMs: Long): List<PluginRegistryEntity> = listOf(
        PluginRegistryEntity(
            pluginId = "meemo.plugin.apps",
            displayName = "Application & Browser Launcher",
            versionName = "1.0.0",
            supportedIntentsCsv = "OPEN_APPLICATION,OPEN_BROWSER",
            requiredPermissionsCsv = "QUERIES_LAUNCHER_INTENT",
            isEnabled = true,
            registeredAtMs = nowMs
        ),
        PluginRegistryEntity(
            pluginId = "meemo.plugin.torch",
            displayName = "Camera2 Flashlight Controller",
            versionName = "1.0.0",
            supportedIntentsCsv = "TORCH_ON,TORCH_OFF",
            requiredPermissionsCsv = "FEATURE_CAMERA_FLASH",
            isEnabled = true,
            registeredAtMs = nowMs
        ),
        PluginRegistryEntity(
            pluginId = "meemo.plugin.deviceinfo",
            displayName = "Battery & Device Diagnostics",
            versionName = "1.0.0",
            supportedIntentsCsv = "GET_BATTERY_STATUS,GET_DEVICE_INFORMATION",
            requiredPermissionsCsv = "NONE",
            isEnabled = true,
            registeredAtMs = nowMs
        ),
        PluginRegistryEntity(
            pluginId = "meemo.plugin.media",
            displayName = "Audio Stream & Media Controls",
            versionName = "1.0.0",
            supportedIntentsCsv = "SET_VOLUME,ADJUST_VOLUME,MEDIA_PLAY,MEDIA_PAUSE,MEDIA_NEXT,MEDIA_PREVIOUS",
            requiredPermissionsCsv = "NONE",
            isEnabled = true,
            registeredAtMs = nowMs
        ),
        PluginRegistryEntity(
            pluginId = "meemo.plugin.notifications",
            displayName = "Privacy-First Notification Read-Aloud",
            versionName = "1.0.0",
            supportedIntentsCsv = "READ_NOTIFICATIONS,STOP_SPEAKING",
            requiredPermissionsCsv = "BIND_NOTIFICATION_LISTENER_SERVICE",
            isEnabled = true,
            registeredAtMs = nowMs
        ),
        PluginRegistryEntity(
            pluginId = "meemo.plugin.root",
            displayName = "Isolated Root Diagnostics Policy",
            versionName = "1.0.0",
            supportedIntentsCsv = "ROOT_SYSTEM_DIAGNOSTIC",
            requiredPermissionsCsv = "ROOT_SU_BINARY",
            isEnabled = true,
            registeredAtMs = nowMs
        )
    )
}
